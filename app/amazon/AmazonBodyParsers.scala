package amazon

import play.api.mvc._
import play.api.libs.iteratee.Iteratee
import akka.actor._
import play.api.http.HeaderNames
import akka.pattern.{ask, pipe}
import scala.concurrent.{Future, ExecutionContext, Await}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps

import scala.util.{Failure, Success, Try}
import spray.http.HttpResponse
import cf.s3.S3P._
import cf.s3.S3StreamPutFSM
import play.api.Logger

/**
 * Created with IntelliJ IDEA.
 * User: cfchou
 * Date: 25/01/2014
 */
object AmazonBodyParsers extends Results {

  import ExecutionContext.Implicits.global
  val system = ActorSystem()

  val expired = 10 seconds
  implicit val timeout = Timeout(expired)

  case class S3Object(bucket: String, key: String, secret: String, dest: String)

  def futureLogging[T](f: Future[T], msg: String): Future[T] = {
    f.transform({ x: T => Logger.debug(s"$msg: $x"); x},
      { e: Throwable => Logger.debug(s"$msg: $e"); e })
  }

  def S3Upload(obj: S3Object) = BodyParser { requestHeader =>
    val s3put = system.actorOf(Props(S3StreamPutFSM(obj.bucket, obj.key,
      obj.secret)).withMailbox("unbounded-deque-based"))
    val ct = requestHeader.headers.get(HeaderNames.CONTENT_TYPE)
    val cl = requestHeader.headers.get(HeaderNames.CONTENT_LENGTH).get.toLong

    val init = futureLogging(s3put ? S3Connect, "** Conn **") flatMap { _ =>
      futureLogging(s3put ? S3ChunkedStart(obj.dest, ct, cl), "** Start **")
    }
    Iteratee.fold[Array[Byte], Future[_]](init) { (f, bytes) =>
      f flatMap { _ =>
        futureLogging({ s3put ? S3ChunkedData(bytes) }, "** Chk **")
      }
    } map { f => // mapDone is deprecated
      val g = f flatMap { _ =>
        futureLogging({ s3put ? S3ChunkedEnd }, "** End **")
      }
      g onComplete { _ =>
        system.stop(s3put)
      }
      Try(Await.result(g, expired)) match {
        case Success(v) => v match {
          case x: HttpResponse if x.status.isSuccess =>
            if (x.status.isSuccess) Right(obj.dest)
            else {
              /* TODO:
               * Cannot write an instance of spray.http.HttpEntity to Http
               * response. Try to define a Writeable[spray.http.HttpEntity]
               */
              // Left(Forbidden(x.entity))
              Left(Forbidden(s"failed -- $x"))
            }
          case x => Left(Forbidden(s"failed -- $x"))
        }
        case Failure(e) => Left(Forbidden(s"failed(exception) -- $e"))
      }
    }
  }
}

