package modules

import com.google.inject.AbstractModule
import models._
import net.codingwell.scalaguice.ScalaModule

/**
 * Created by alex on 28/09/15.
 */
class AppModule extends AbstractModule with ScalaModule {

  /**
   * Configures the module.
   */
  def configure() {
    bind[UserService].to[TraineeService]
    bind[AddressService].to[AddressServiceTraineeImpl]
  }
}
