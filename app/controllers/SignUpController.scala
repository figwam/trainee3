package controllers

import java.sql.Timestamp
import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import forms.{SignUpForm}
import models.daos.TraineeDAO
import models.{Address, Trainee}
import models.services.TraineeService
import play.api.i18n.{ MessagesApi }
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc.Action

import scala.concurrent.Future

/**
 * The sign up controller.
 *
 * @param messagesApi The Play messages API.
 * @param env The Silhouette environment.
 * @param traineeService The trainee service implementation.
 * @param authInfoRepository The auth info repository implementation.
 * @param avatarService The avatar service implementation.
 * @param passwordHasher The password hasher implementation.
 */
class SignUpController @Inject() (
  val messagesApi: MessagesApi,
  val env: Environment[Trainee, JWTAuthenticator],
  traineeService: TraineeService,
  traineeDAO: TraineeDAO,
  authInfoRepository: AuthInfoRepository,
  avatarService: AvatarService,
  passwordHasher: PasswordHasher)
  extends Silhouette[Trainee, JWTAuthenticator] {

  /**
   * Registers a new trainee.
   *
   * @return The result to display.
   */
  def signUp = Action.async(parse.json) { implicit request =>
    request.body.validate[SignUpForm.Data].map { data =>
      val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
      traineeService.retrieve(loginInfo).flatMap {
        case Some(trainee) =>
          Future.successful(Unauthorized(Json.obj("message" -> "trainee.exists")))
        case None =>
          val authInfo = passwordHasher.hash(data.password)
          val addr = Address(None, data.street, data.city, data.zip, data.state, "Switzerland")
          val trainee = Trainee(
            None,
            loginInfo = loginInfo,
            firstname = Some(data.firstname),
            lastname = Some(data.lastname),
            mobile = None,
            phone = None,
            email = Some(data.email),
            emailVerified = false,
            createdOn = new Timestamp(System.currentTimeMillis()),
            updatedOn = new Timestamp(System.currentTimeMillis()),
            ptoken = None,
            isActive = true,
            inactiveReason = None,
            username = Some (data.email),
            fullname = Some(data.firstname + " " + data.lastname),
            avatarurl = None,
            address = addr
          )
          for {
            avatar <- avatarService.retrieveURL(data.email)
            trainee <- traineeService.save(trainee.copy(avatarurl = avatar))
            authInfo <- authInfoRepository.add(loginInfo, authInfo)
            authenticator <- env.authenticatorService.create(loginInfo)
            token <- env.authenticatorService.init(authenticator)
          } yield {
            env.eventBus.publish(SignUpEvent(trainee, request, request2Messages))
            env.eventBus.publish(LoginEvent(trainee, request, request2Messages))
            Ok(Json.obj("token" -> token))
          }
      }
    }.recoverTotal {
      case error =>
        Future.successful(Unauthorized(Json.obj("message" -> "invalid.data", "detail" -> JsError.toJson(error))))
    }
  }
}
