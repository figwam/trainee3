package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import forms.SignUpForm
import models._
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json._
import play.api.mvc.Action
import utils.{MailService, MailTokenService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.twirl.api.Html
import play.api.i18n.Messages
import views.html.mails

/**
  * The sign up controller.
  *
  * @param messagesApi The Play messages API.
  * @param env The Silhouette environment.
  * @param service The trainee service implementation.
  * @param authInfoRepository The auth info repository implementation.
  * @param avatarService The avatar service implementation.
  * @param passwordHasher The password hasher implementation.
  */
class SignUpController @Inject()(
                                  val messagesApi: MessagesApi,
                                  val env: Environment[User, JWTAuthenticator],
                                  service: TraineeService,
                                  authInfoRepository: AuthInfoRepository,
                                  avatarService: AvatarService,
                                  passwordHasher: PasswordHasher,
                                  val ms: MailService,
                                  val tokenService: MailTokenService[MailTokenUser])
  extends Silhouette[User, JWTAuthenticator] {

  /**
    * Registers a new trainee.
    *
    * @return The result to display.
    */
  def signUp = Action.async(parse.json) { implicit request =>
    request.body.validate[SignUpForm.Data].map { data =>
      val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
      service.retrieve(loginInfo).flatMap {
        case Some(trainee) =>
          Future.successful(Unauthorized(Json.obj("message" -> "trainee.exists")))
        case None =>
          val authInfo = passwordHasher.hash(data.password)
          val addr = Address(None, data.street, data.city, data.zip, data.state, "Switzerland")
          val user = Trainee(
            firstname = Some(data.firstname),
            lastname = Some(data.lastname),
            email = Some(data.email),
            username = Some(data.email),
            fullname = Some(data.firstname + " " + data.lastname)
          )
          val mailtoken = MailTokenUser(data.email, isSignUp = true)

          for {
            avatar <- avatarService.retrieveURL(data.email)
            trainee <- service.signUp(user.copy(avatarurl = avatar), loginInfo, addr)
            authInfo <- authInfoRepository.add(loginInfo, authInfo)
            authenticator <- env.authenticatorService.create(loginInfo)
            logintoken <- env.authenticatorService.init(authenticator)
            _ <- tokenService.create(mailtoken)
          } yield {
            env.eventBus.publish(SignUpEvent(trainee, request, request2Messages))
            env.eventBus.publish(LoginEvent(trainee, request, request2Messages))
            ms.sendEmailAsync(data.email)(
              subject = Messages("mail.welcome.subject"),
              bodyHtml = mails.welcome(data.firstname, routes.SignUpController.signUpConfirm(mailtoken.id).absoluteURL()).toString(),
              bodyText = mails.welcomeTxt(data.firstname, routes.SignUpController.signUpConfirm(mailtoken.id).absoluteURL()).toString()
            )
            Ok(Json.obj("token" -> logintoken))
          }
      }
    }.recoverTotal {
      case error =>
        Future.successful(Unauthorized(Json.obj("message" -> "invalid.data", "detail" -> JsError.toJson(error))))
    }
  }


  def signUpConfirm(tokenId: String) = Action.async { implicit request =>
    tokenService.retrieve(tokenId).flatMap {
      case Some(token) if token.isSignUp && !token.isExpired => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, token.email)
        service.retrieve(loginInfo).flatMap {
          case Some(user) => { user match {
            case t:Trainee => {
              service.update(t.copy(emailVerified = Some(true))).flatMap {
                case obj:Trainee =>
                  Future.successful(Ok(Json.obj("message" -> Messages("email.verify.success"))))
                case _ =>
                  logger.error("Email verification failed")
                  Future.successful(InternalServerError(Json.obj("message" -> Messages("email.verify.fail"))))
              }
            }
          }

          }
          case None => Future.successful(Ok(Json.obj("message" -> "trainee.NONE.Texists")))
        }
      }
      case Some(token) => {
        tokenService.consume(tokenId)
        Future.successful(Ok(Json.obj("message" -> "trainee.NOT.SOME.exists")))
      }
      case None => Future.successful(Ok(Json.obj("message" -> "trainee.NOT.NONE.exists")))
    }
  }
}
