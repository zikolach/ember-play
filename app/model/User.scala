package model

import scala.concurrent.{Promise, ExecutionContext, Future}
import ExecutionContext.Implicits.global
import scala.util.Random
import controllers.CrudDAO
import play.api.libs.json.Json

case class Address(city: String, street: String)

case class User(id: Option[Long], name: String, address: Option[Address]) {

}

object User extends CrudDAO[Long, User] {

  type UserNotFoundException = Exception

  implicit val addressFormat = Json.format[Address]
  implicit val userFormat = Json.format[User]

  var users: Map[Long, User] = (1 to Random.nextInt(10) + 1).map(
    (n: Int) => n.toLong -> User(
      Some(n),
      Random.alphanumeric.take(Random.nextInt(10)).mkString,
      Some(Address(
        Random.alphanumeric.take(Random.nextInt(10)).mkString,
        Random.alphanumeric.take(Random.nextInt(10)).mkString
      ))
    )
  ).toMap


  def all(): Future[List[User]] = Future {
    users.values.toList
  }

  def get(id: Long): Future[User] = {
    val p = Promise[User]()
    Future {
      users.get(id) match {
        case Some(user) => p.success(user)
        case None => p.failure(new UserNotFoundException)
      }
    }
    p.future
  }

  def create(user: User): Future[(Long, User)] = Future {
    val id = users.keys.max + 1
    val userWithId = User(Some(id), user.name, user.address)
    users = users.updated(id, userWithId)
    (id, userWithId)
  }

  def update(id: Long, user: User): Future[(Long, User)] = {
    val p = Promise[(Long, User)]()
    Future {
      users.get(id) match {
        case Some(u) =>
          val updatedUser = u.copy(name = user.name, address = user.address)
          users = users.updated(id, updatedUser)
          p.success((id, updatedUser))
        case None => p.failure(new UserNotFoundException("User not found"))
      }
    }
    p.future
  }

  def delete(id: Long): Future[User] = {
    val p = Promise[User]()
    Future {
      users.get(id) match {
        case Some(user) =>
//          users = (users - id).toMap
          users -= id
          p.success(user)
        case None => p.failure(new UserNotFoundException)
      }
    }
    p.future
  }

}