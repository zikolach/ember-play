package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.index(flash.get("message").getOrElse("Welcome!!!")))
  }

}