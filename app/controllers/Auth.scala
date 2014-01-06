package controllers

import play.api.mvc._
import play.api.libs.json.Json
import scala.Some
import model.{Token, User}
import scala.concurrent.{Promise, ExecutionContext, Future}
import ExecutionContext.Implicits.global
import scala.util.{Random, Failure, Success}
import model.User.UserNotFoundException
import service.Mail._
import service._

object Account {

  object UserAlreadyExists extends Exception("User already exists")

  def register(email: String, password: String): Future[User] = {
    val p = Promise[User]()
    User.findByEmail(email).onComplete {
      case Success(user) => p.failure(UserAlreadyExists)
      case Failure(UserNotFoundException) =>
        User.create(User(
          id = None,
          email = Some(email),
          password = Some(password),
          code = Some(Random.alphanumeric.take(5).mkString)
        )).onComplete {
          case Success((_, user)) =>
            for {
              e <- user.email
              p <- user.password
              c <- user.code
            } sendConfirmation(e, e, p, c)
            p.success(user)
          case Failure(err) => p.failure(err)
        }
      case Failure(err) => p.failure(err)
    }
    p.future
  }

  def login(email: String, password: String, addr: String): Future[Token] =
    User.findByEmailAndPassword(email, password) flatMap {
      case User(Some(id), _, _, _, _, _, _) => Token.create(id, addr)
    }

  def login(token: String): Future[Token] = Token.get(token)

  def logout(token: String): Future[Unit] = {
    Token.get(token) flatMap {
      case _ => Token.delete(token)
    }
  }

  def confirm(code: String): Future[User] =
    User.findByCode(code)

  private def sendConfirmation(email: String, user: String, password: String, code: String) = send a new Mail(
    from = (service.Mail.accountName, appName),
    to = email,
    subject = "Confirmation email",
    message =
      s"""Dear user,
        |
        |You have been successfully registered.
        |
        |Login: $user
        |Password: $password
        |To confirm your email click following link: http://localhost:8080/api/v1/confirm?code=$code
        |
        |Your $appName
        |
      """.stripMargin
  )

}

trait Authorization {

  case class ResponseWrapper(status: Option[String])

  def checkToken(headers: Headers): Future[Boolean] = headers.get("token") match {
    case Some(token) => User.findByToken(token) map {
      _ => true
    } recover {
      case _ => false
    }
    case None => Future {
      false
    }
  }

  def getAuthor(headers: Headers): Future[User] = headers.get("token") match {
    case Some(token) => User.findByToken(token)
    case None => throw UserNotFoundException
  }

  def safeUser(user: User): User = user.copy(
    password = None
  )
}

object Auth extends Controller {

  case class AuthResponse(token: Option[String] = None,
                          userId: Option[Long] = None,
                          message: Option[String] = None)

  case class AuthRequest(email: Option[String], password: Option[String], token: Option[String])


  implicit val tokenFormat = Json.format[Token]
  implicit val authResponseFormat = Json.format[AuthResponse]
  implicit val authRequestFormat = Json.format[AuthRequest]

  def register = Action.async(parse.json) {
    request => {
      request.body.validate[AuthRequest].map({
        case AuthRequest(Some(email), Some(password), None) => {
          Account.register(email, password) map {
            case user => Ok(Json.toJson(AuthResponse(message = Some(s"User successfully registered and confirmation letter sent to $email"))))
          } recover {
            case err => BadRequest(Json.toJson(AuthResponse(None, None, Some(err.getMessage))))
          }
        }
      }).recoverTotal(
        e => Future {
          BadRequest(Json.toJson(AuthResponse(None, None, Some("Invalid request"))))
        }
      )
    }
  }

  def login = Action.async(parse.json) {
    request => {
      request.body.validate[AuthRequest].map({
        case AuthRequest(Some(email), Some(password), None) => {
          Account.login(email, password, request.remoteAddress) map {
            case token =>
              Ok(Json.toJson(AuthResponse(
                token = Some(token.token),
                userId = Some(token.userId),
                message = Some("Successfully logged in")
              )))
          } recover {
            case err => BadRequest(Json.toJson(AuthResponse(message = Some(err.getMessage))))
          }
        }
        case AuthRequest(None, None, Some(existingToken)) => {
          Account.login(existingToken) map {
            case token => Ok(Json.toJson(AuthResponse(
              token = Some(token.token),
              userId = Some(token.userId),
              message = Some("Successfully logged in")
            )))
          } recover {
            case err => BadRequest(Json.toJson(AuthResponse(message = Some(err.getMessage))))
          }
        }
      }).recoverTotal(
        e => Future {
          BadRequest(Json.toJson(AuthResponse(None, None, Some("Invalid request"))))
        }
      )
    }
  }

  def logout = Action.async {
    request => {
      request.headers.get("token") match {
        case Some(token) => {
          Account.logout(token) map {
            case _ => Ok(Json.toJson(AuthResponse(None, None, Some("Successfully logged out"))))
          } recover {
            case err => BadRequest(Json.toJson(AuthResponse(None, None, Some(err.getMessage))))
          }
        }
        case None => Future {
          BadRequest(Json.toJson(AuthResponse(None, None, Some("Invalid token"))))
        }
      }
    }
  }

  def confirm = Action.async {
    request =>
      request.getQueryString("code") match {
        case Some(code) => Account.confirm(code) map {
          case user =>
            Redirect("/").flashing("message" -> "Your registration was successful.")
        } recover {
          case err => Redirect("/").flashing("message" -> err.getMessage)
        }
        case None => Future {
          Redirect("/").flashing("message" -> "Confirmation code is not specified.")
        }
      }
  }

}
