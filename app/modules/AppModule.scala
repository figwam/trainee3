package modules

import com.google.inject.AbstractModule
import models.daos._
import net.codingwell.scalaguice.ScalaModule

/**
 * Created by alex on 28/09/15.
 */
class AppModule extends AbstractModule with ScalaModule {

  /**
   * Configures the module.
   */
  def configure() {
    bind[ClazzDAO].to[ClazzDAOImpl]
    bind[TraineeDAO].to[TraineeDAOImpl]
    bind[ClazzDefinitionDAO].to[ClazzDefinitionDAOImpl]
    bind[OfferDAO].to[OfferDAOImpl]
    bind[RegistrationDAO].to[RegistrationDAOImpl]
    bind[LoggerDAO].to[LoggerDAOImpl]
    bind[PartnerDAO].to[PartnerDAOImpl]
    bind[SubscriptionDAO].to[SubscriptionDAOImpl]
    bind[BillDAO].to[BillDAOImpl]
  }
}
