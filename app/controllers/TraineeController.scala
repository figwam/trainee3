package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import forms.TraineeForm
import models._
import models.daos._
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.{JsSuccess, JsError, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


@Singleton
class TraineeController @Inject()(
                                   val messagesApi: MessagesApi,
                                   val env: Environment[Trainee, JWTAuthenticator],
                                   traineeDAO: TraineeDAO)
  extends Silhouette[Trainee, JWTAuthenticator] {


  def update() = SecuredAction.async(parse.json) { implicit request =>
    request.body.validate[TraineeForm.Data] match {
      case error: JsError => {
        Future.successful(BadRequest(Json.obj("message" -> Messages("save.fail"), "detail" -> JsError.toJson(error))))
      }
      case s: JsSuccess[TraineeForm.Data] => {
        request.body.validate[Trainee].map { trainee =>
          traineeDAO.update(trainee).flatMap {
            case t:Trainee =>
              Future.successful(Ok(Json.obj("message" -> Messages("save.ok"))))
            case _ =>
              Future.successful(BadRequest(Json.obj("message" -> Messages("save.fail"))))
          }
        }.recoverTotal {
          case error =>
            Future.successful(BadRequest(Json.obj("message" -> "invalid.data", "detail" -> JsError.toJson(error))))
        }
      }
    }
  }

}
