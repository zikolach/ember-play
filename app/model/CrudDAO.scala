package model

import scala.concurrent.Future

trait CrudDAO[Key, Value] {
  def all(): Future[List[Value]]

  def get(id: Key): Future[Value]

  def create(value: Value): Future[(Key, Value)]

  def update(id: Key, value: Value): Future[(Key, Value)]

  def delete(id: Key): Future[Value]
}
