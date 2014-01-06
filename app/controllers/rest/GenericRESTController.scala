package controllers.rest

import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import play.api.data.validation.ValidationError

trait CrudDAO[Key, Value] {
  def all(): Future[List[Value]]

  def get(id: Key): Future[Value]

  def create(value: Value): Future[(Key, Value)]

  def update(id: Key, value: Value): Future[(Key, Value)]

  def delete(id: Key): Future[Value]
}

trait CrudREST[Key <: Long, Value] {
  def index: Action[Option[Any]]

  def get(id: Key): Action[Option[Any]]

  def create: Action[JsValue]

  def update(id: Key): Action[JsValue]

  def delete(id: Key): Action[Option[Any]]
}

abstract class GenericRESTController[Key <: Long, Value](val dao: CrudDAO[Key, Value])
                                                        (implicit val valueFormat: Format[Value], implicit val ct: scala.reflect.ClassTag[Value])
  extends Controller with CrudREST[Key, Value] {

  val root = scala.reflect.classTag[Value].runtimeClass.getSimpleName.toLowerCase + "s"

  implicit class ResponseParseError(err: Seq[(JsPath, Seq[ValidationError])]) {
    def toJson: JsValue = Json.obj(
      "code" -> -1,
      "message" -> "Json parse error",
      "errors" -> JsError.toFlatJson(err)
    )
  }

  implicit class ResponseError(err: Throwable) {
    def toJson: JsValue = Json.obj(
      "code" -> -2,
      "message" -> err.getMessage,
      "errors" -> err.getStackTraceString
    )
  }

  val outTransformer = (__ \ root).json.copyFrom(__.json.pick.map {
    case arr: JsArray => arr
    case other => Json.arr(other)
  })
  val inTransformer = (__ \ root).json.pick


  def wrapResponse(value: Value) = Json.toJson(value).transform(outTransformer).fold(
    valid = json => Ok(json),
    invalid = err => BadRequest(err.toJson)
  )

  def wrapResponse(values: List[Value]) = Json.toJson(values).transform(outTransformer).fold(
    valid = json => Ok(json),
    invalid = err => BadRequest(err.toJson)
  )

  def index = Action.async(parse.empty) {
    request =>
      dao.all() map {
        values =>
          Json.toJson(values).transform(outTransformer).fold(
            valid = json => Ok(json),
            invalid = err => BadRequest(err.toJson)
          )
      } recover {
        case err => BadRequest(err.toJson)
      }
  }

  def get(id: Key) = Action.async(parse.empty) {
    request =>
      dao.get(id).map {
        values => Json.toJson(values).transform(outTransformer).fold(
          valid = json => Ok(json),
          invalid = err => BadRequest(err.toJson)
        )
      } recover {
        case err => BadRequest(err.toJson)
      }
  }

  def create = Action.async(parse.json) {
    request =>
      request.body.transform(inTransformer).fold(
        valid = json => json.validate[Value].fold(
          valid = value => dao.create(value) map {
            case (_, v) => Json.toJson(v).transform(outTransformer).fold(
              valid = json => Ok(json),
              invalid = err => BadRequest(err.toJson)
            )
          } recover {
            case err => BadRequest(err.toJson)
          },
          invalid = err => Future {
            BadRequest(err.toJson)
          }
        ),
        invalid = err => Future {
          BadRequest(err.toJson)
        }
      )
  }

  def update(id: Key) = Action.async(parse.json) {
    request =>
      request.body.transform(inTransformer).fold(
        valid = json => json.validate[Value].fold(
          valid = value => dao.update(id, value) map {
            case (_, v) => Json.toJson(v).transform(outTransformer).fold(
              valid = json => Ok(json),
              invalid = err => BadRequest(err.toJson)
            )
          } recover {
            case err => BadRequest(err.toJson)
          },
          invalid = err => Future {
            BadRequest(err.toJson)
          }
        ),
        invalid = err => Future {
          BadRequest(err.toJson)
        }
      )
  }

  def delete(id: Key) = Action.async(parse.empty) {
    request =>
      dao.delete(id) map {
        case _ => Json.toJson(Seq()).transform(outTransformer).fold(
          valid = json => Ok(json),
          invalid = err => BadRequest(err.toJson)
        )
      } recover {
        case err => BadRequest(err.toJson)
      }
  }

}
