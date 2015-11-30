package forms

import play.api.libs.json._
import play.api.libs.functional.syntax._
import Reads._

/**
 * The form which handles the sign up process.
 */
object TraineeForm {


  case class Data(
                    firstname: String,
                    lastname: String,
                    address: DataAddress)

  case class DataAddress( street: String,
                    city: String,
                    zip: String)


  object Data {
    implicit val traineeReads = (
      (__ \ 'firstname).read[String](minLength[String](1)) and
        (__ \ 'lastname).read[String](minLength[String](1)) and
          (__ \ 'address).read[DataAddress]
      )(TraineeForm.Data.apply _)
  }

  object DataAddress {
    implicit val addressReads: Reads[DataAddress] = (
      (__ \ 'street).read[String](minLength[String](1)) and
        (__  \'city).read[String](minLength[String](1)) and
        (__ \'zip).read[String](verifying[String](_.matches("\\d{4,4}")))
      )(TraineeForm.DataAddress.apply _)
  }
}
