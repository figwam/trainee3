package controllers

import java.sql.Timestamp
import java.util.{Date, GregorianCalendar, UUID}
import java.util.concurrent.TimeoutException
import javax.inject.{Singleton, Inject}

import com.mohiva.play.silhouette.api.{ Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models._
import models.daos._
import play.Play
import play.api.Logger
import play.api.cache.Cache
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import scala.concurrent.duration._

import scala.concurrent.{Await, Future}

/**
 * The basic application controller.
 *
 * @param messagesApi The Play messages API.
 * @param env The Silhouette environment.
 * @param socialProviderRegistry The social provider registry.
 */
@Singleton
class ApplicationController @Inject()(
                                       val messagesApi: MessagesApi,
                                       val env: Environment[Trainee, JWTAuthenticator],
                                       socialProviderRegistry: SocialProviderRegistry,
                                       clazzDAO: ClazzDAO,
                                       traineeDAO: TraineeDAO,
                                       offerDAO: OfferDAO,
                                       subscriptionDAO: SubscriptionDAO)
  extends Silhouette[Trainee, JWTAuthenticator] {

  def offers = UserAwareAction.async { implicit request =>
    lazy val cacheExpire = Play.application().configuration().getString("cache.expire.get.offers").toInt
    val offers:List[Offer] = Cache.getAs[List[Offer]]("offers").getOrElse{
      val offers:List[Offer] = Await.result(offerDAO.list(), 5.seconds)
      Cache.set("offers", offers, cacheExpire.seconds)
      offers
    }
    Future.successful(Ok(Json.toJson(offers)))
  }

  /**
   * Returns the trainee.
   *
   * @return The result to display.
   */
  def trainee = SecuredAction.async { implicit request =>
    Future.successful(Ok(Json.toJson(request.identity)))
  }

    /*
    try {
      (request.body \ "idTrainee").asOpt[Long].map { idTrainee =>
        val future = traineeDAO.book(Registration(None, UUID.randomUUID(), idTrainee, idClazz))
        future.onSuccess { case a => Logger.debug(s"Registration created: $a"); Future.successful(Ok)}
        future.onFailure {
          case t: PSQLException => {
            if (t.getMessage.contains("duplicate key value violates unique constraint")) {
              Logger.info("Registration already exists")
              Future.successful(BadRequest("registration already exists"))
            }
            else {
              Logger.error("Something bad happened", t)
              Future.successful(InternalServerError("Something bad happened"))
            }
          }
          case t: Throwable => {
            Logger.error(t.getMessage,t)
            Future.successful(InternalServerError("Something bad happened"))
          }
          case _ => Future.successful(BadRequest("Missing parameter [idTrainee]"))
        }
      }.getOrElse {
        Future.successful(BadRequest("Missing parameter [idTrainee]"))
      }
    } catch {
      case t: Throwable =>
        Logger.error(t.getMessage,t)
        Future.successful(InternalServerError("Something bad happened"))
    }
    */



  def clazzes(page: Int, orderBy: Int, filter: String) = UserAwareAction.async { implicit request =>
    clazzDAO.list(page, 10, orderBy, "%" + filter + "%").flatMap { pageClazzes =>
      Future.successful(Ok(Json.toJson(pageClazzes)))
    }.recover {
      case ex: TimeoutException =>
        Logger.error("Problem found in clazz list process")
        InternalServerError(ex.getMessage)
    }
  }

  def clazzesCount = UserAwareAction.async { implicit request =>
    clazzDAO.count.flatMap{ count =>
      Future.successful(Ok(Json.toJson(count)))
    }
  }


  def clazzesPersonalizedAll(page: Int, orderBy: Int, filter: String) = SecuredAction.async { implicit request =>
    clazzDAO.listPersonalizedAll(page, 10, orderBy, "%" + filter + "%", request.identity.id.getOrElse(UUID.randomUUID())).flatMap { pageClazzes =>
      Future.successful(Ok(Json.toJson(pageClazzes)))
    }.recover {
      case ex: TimeoutException =>
        Logger.error("Problem found in clazz list process")
        InternalServerError(ex.getMessage)
    }
  }



  def clazzesPersonalizedMy(page: Int, orderBy: Int, filter: String, startFrom: Long, endAt:Long) = SecuredAction.async { implicit request =>
    val d = new GregorianCalendar()
    d.setTimeInMillis(startFrom)
    clazzDAO.listPersonalizedMy(page, 10, orderBy, "%" + filter + "%", request.identity.id.getOrElse(UUID.randomUUID()), new Timestamp(startFrom), new Timestamp(endAt)).flatMap { pageClazzes =>
      Future.successful(Ok(Json.toJson(pageClazzes)))
    }.recover {
      case ex: TimeoutException =>
        Logger.error("Problem found in clazz list process")
        InternalServerError(ex.getMessage)
    }
  }

  /**
   * Manages the sign out action.
   */
  def signOut = SecuredAction.async { implicit request =>
    env.eventBus.publish(LogoutEvent(request.identity, request, request2Messages))
    env.authenticatorService.discard(request.authenticator, Ok)
  }

  /**
   * Provides the desired template.
   *
   * @param template The template to provide.
   * @return The template.
   */

  def viewRestricted(template: String) = SecuredAction.async { implicit request =>
    template match {
      case "clazzes" => Future.successful(Ok(views.html.me.clazzes()))
      case "myclazzes" => Future.successful(Ok(views.html.me.myclazzes()))
      case "subscription" => Future.successful(Ok(views.html.me.subscription()))
      case "dashboard" => Future.successful(Ok(views.html.me.dashboard()))
      case "header" => Future.successful(Ok(views.html.me.header()))
      case "sidebar" => Future.successful(Ok(views.html.me.sidebar()))
      case "profile" => Future.successful(Ok(views.html.me.profile()))
      case "bill" => Future.successful(Ok(views.html.me.bill()))
      case "deleteSubscriptionModal" => Future.successful(Ok(views.html.templates.modal("danger", Messages("my.subscription.delete.modal.header"), Messages("my.subscription.delete.modal.body"), Messages("word.cancel"), Messages("word.confirm"))))
      case _ => Future.successful(NotFound)
    }
  }

  def view(template: String) = UserAwareAction { implicit request =>
    template match {
      case "test" => Ok(views.html.me.subscription())
      case "home" => Ok(views.html.home())
      case "signUp" => Ok(views.html.signUp())
      case "signUpAbo" => Ok(views.html.signUpAbo())
      case "signUpProfile" => Ok(views.html.signUpProfile())
      case "signUpPayment" => Ok(views.html.signUpPayment())
      case "signIn" => Ok(views.html.signIn(socialProviderRegistry))
      case "clazzes" => Ok(views.html.clazzes())
      case "header" => Ok(views.html.header())
      case "footer" => Ok(views.html.footer())
      case _ => NotFound
    }
  }
}
