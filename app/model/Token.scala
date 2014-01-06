package model

import java.util.{UUID, Date}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class Token(token: String, userId: Long, createdAt: Date, ipAddr: String)

object Token {

  // TODO: implement persistent storage
  var tokens: Map[String, Token] = Map.empty

  def create(userId: Long, ipAddr: String): Future[Token] = Future {
    val token = UUID.randomUUID().toString
    val createdAt = new Date
    tokens += token -> Token(token, userId, createdAt, ipAddr)
    tokens(token)
  }

  def delete(token: String): Future[Unit] = Future {
    tokens -= token
  }

  def get(token: String): Future[Token] = Future {
    tokens(token)
  }

}