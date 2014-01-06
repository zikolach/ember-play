package model

import scala.concurrent.{Promise, ExecutionContext, Future}
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Random}
import play.api.libs.json.Json
import controllers.rest.CrudDAO

case class Address(city: String, street: String)

case class Names(firstName: String, lastName: String)

case class User(id: Option[Long],
                email: Option[String],
                password: Option[String],
                code: Option[String] = Some(Random.alphanumeric.take(5).mkString),
                confirmed: Option[Boolean] = Some(false),
                names: Option[Names] = None,
                address: Option[Address] = None)

object User extends CrudDAO[Long, User] {

  object UserNotFoundException extends Exception("User not found")

  implicit val addressFormat = Json.format[Address]
  implicit val namesFormat = Json.format[Names]
  implicit val userFormat = Json.format[User]

  var users: Map[Long, User] = (1 to Random.nextInt(10) + 1).map(
    (n: Int) => n.toLong -> User(
      id = Some(n),
      email = Some(Random.alphanumeric.take(Random.nextInt(10)).mkString),
      password = Some(Random.alphanumeric.take(Random.nextInt(10)).mkString),
      names = Some(Names(
        Random.alphanumeric.take(Random.nextInt(10)).mkString,
        Random.alphanumeric.take(Random.nextInt(10)).mkString
      )),
      address = Some(Address(
        Random.alphanumeric.take(Random.nextInt(10)).mkString,
        Random.alphanumeric.take(Random.nextInt(10)).mkString
      ))
    )
  ).toMap


  def all(): Future[List[User]] = Future {
    users.values.toList
  }

  def get(id: Long): Future[User] = Future {
    users.get(id) match {
      case Some(user) => user
      case None => throw UserNotFoundException
    }
  }

  def create(user: User): Future[(Long, User)] = Future {
    val id = users.keys.max + 1
    val userWithId = user.copy(id = Some(id))
    users = users.updated(id, userWithId)
    (id, userWithId)
  }

  def update(id: Long, user: User): Future[(Long, User)] = Future {
    users.get(id) match {
      case Some(u) =>
        val updatedUser = u.copy(
          email = user.email,
          password = user.password,
          code = user.code,
          names = user.names,
          address = user.address)
        users = users.updated(id, updatedUser)
        (id, updatedUser)
      case None => throw UserNotFoundException
    }
  }

  def delete(id: Long): Future[User] = Future {
    users.get(id) match {
      case Some(user) =>
        users -= id
        user
      case None => throw UserNotFoundException
    }
  }

  // utility methods

  def findByCode(code: String): Future[User] = Future {
    users.values.find(_.code == Some(code)) match {
      case Some(user) => user
      case None => throw UserNotFoundException
    }
  }

  def findByEmailAndPassword(email: String, password: String): Future[User] = Future {
    users.values.find(u => u.email == Some(email) && u.password == Some(password)) match {
      case Some(user) => user
      case None => throw UserNotFoundException
    }
  }

  def findByEmail(email: String): Future[User] = Future {
    users.values.find(u => u.email == Some(email)) match {
      case Some(user) => user
      case None => throw UserNotFoundException
    }
  }

  def findByToken(tokenString: String): Future[User] = {
    val p = Promise[User]()
    Token.get(tokenString).onComplete {
      case Success(token) => users.get(token.userId) match {
        case Some(user) => p.success(user)
        case None => p.failure(UserNotFoundException)
      }
      case Failure(err) => p.failure(err)
    }
    p.future
  }

}