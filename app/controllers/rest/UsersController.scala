package controllers.rest

import model.User
import controllers.Authorization
import play.api.mvc._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

object UsersController extends GenericRESTController[Long, User](User) with Authorization {

  override def index = Action.async(parse.empty) {
    request => getAuthor(request.headers) map {
      case user => wrapResponse(safeUser(user))
    }
  }

  override def get(id: Long): Action[Option[Any]] = index

}
