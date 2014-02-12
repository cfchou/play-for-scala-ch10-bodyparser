package controllers

import play.api._
import play.api.mvc._
import play.api.mvc.BodyParsers.parse

/**
 * Created with IntelliJ IDEA.
 * User: cfchou
 * Date: 25/01/2014
 */
object FileUpload extends Controller {
  def index = Action {
    Ok(views.html.fileupload.index())
  }

  def file() = Action(parse.multipartFormData) { request =>
    request.body.file("file").map({ filePart =>
      val f = filePart.ref.file
      Ok("File size %s bytes" format f.length())
    }) getOrElse(BadRequest("File missing!"))
  }
}
