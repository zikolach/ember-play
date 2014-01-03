package controllers.rest

import model.User
import controllers.GenericRESTController

object UsersController extends GenericRESTController[Long, User](User) {

}
