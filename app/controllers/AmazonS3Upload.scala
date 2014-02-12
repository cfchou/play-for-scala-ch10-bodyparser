package controllers

import play.api.mvc.{Action, Controller}
import play.api.Play
import play.api.Play.current
import amazon.AmazonBodyParsers
import amazon.AmazonBodyParsers.S3Object

/**
 * Created with IntelliJ IDEA.
 * User: cfchou
 * Date: 25/01/2014
 */
object AmazonS3Upload extends Controller {

  private val destConfigured: Option[(String, String)] = {
    for {
      k <- Play.configuration.getString("s3.bucket") if !k.isEmpty
      s <- Play.configuration.getString("uploadTest.dest") if !s.isEmpty
    } yield (k, s)
  }

  private val credentialsConfigured: Option[(String, String)] = {
    for {
      k <- Play.configuration.getString("aws.key") if !k.isEmpty
      s <- Play.configuration.getString("aws.secret") if !s.isEmpty
    } yield (k, s)
  }

  private val sprayConfigured: Option[Boolean] =
    Play.configuration.getBoolean("spray.can.client.chunkless-streaming")


  def index = Action {
    val maybeErr =
      if(credentialsConfigured.isEmpty)
        Some("Need non-empty aws key and secret")
      else if (destConfigured.isEmpty)
        Some("Need non-empty bucket/destination name")
      else if (sprayConfigured.isEmpty)
        Some("Need to turn on spray-can's chunkless-streaming")
      else
        None
    Ok(views.html.amazons3upload.index(maybeErr))
  }


  val obj = S3Object(destConfigured.get._1, credentialsConfigured.get._1,
    credentialsConfigured.get._2, destConfigured.get._2)
  def file() = Action(AmazonBodyParsers.S3Upload(obj)) { request =>
      Ok("Your file has been uploaded to Amazon, with filename '%s'"
        format request.body)
  }

}

