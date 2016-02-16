package models

import java.sql.Timestamp
import java.util.UUID
import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
  * Trainee implementation of the Clazz ressource.
  *
  * @param dbConfigProvider
  */
class ClazzServiceTraineeImpl @Inject()(override protected val dbConfigProvider: DatabaseConfigProvider)
  extends ClazzServiceImpl(dbConfigProvider: DatabaseConfigProvider) with DAOSlick {

  import driver.api._



  override def listPersonalizedAll(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = "%", idUser: UUID): Future[Page] = {
    val offset = if (page > -1) pageSize * page else 0
    /*
    The following query is executed, which returns all personalized clazzes and additionally the reservation id.

        SELECT id, ext_id, start_from, end_at, name, contingent,
          avatarurl, description, tags, search_meta, nr_of_regs, id_clazzdef, id_studio, id_trainee, id_registration
        FROM (SELECT
                c.id AS cid,
                t.id AS id_trainee,
                r.id AS id_registration
              FROM clazz c, registration r, trainee t
              WHERE r.id_trainee = t.id AND r.id_clazz = c.id) a
          RIGHT OUTER JOIN clazz_view b
            ON b.id = a.cid and id_trainee = 4;
     */

    val regAction = (for {
      trainee <- slickTrainees
      reg <- slickRegistrations.filter(_.idTrainee === trainee.id).filter(_.idTrainee === idUser)
      clazz1 <- slickClazzes.filter(_.id === reg.idClazz)
    } yield (reg))

    val clazzAction = (for {
      (registration, clazz) <- regAction joinRight slickClazzViews
        .sortBy(r => orderBy match {case 1 => r.startFrom case _ => r.startFrom})
        .filter(_.startFrom >= new Timestamp(System.currentTimeMillis()))
        .filter(_.searchMeta.toLowerCase like filter.toLowerCase) on (_.idClazz === _.id)
      s <- slickStudios.filter(_.id === clazz.idStudio)
      a <- slickAddresses.filter(_.id === s.idAddress)
    //(clazz, registrations) <- slickClazzViews.sortBy(r => orderBy match {case 1 => r.startFrom case _ => r.startFrom}) joinRight slickRegistrations on (_.id === _.idClazz)
    //if clazz.startFrom >= new Timestamp(System.currentTimeMillis()) if clazz.searchMeta.toLowerCase like filter.toLowerCase
    } yield (clazz, registration, s, a)).drop(offset).take(pageSize)
    val totalRows = count(filter)


    val result = db.run(clazzAction.result)
    result.map { clazz =>
      clazz.map {
        // go through all the DBClazzes and map them to Clazz
        case (clazz, registration, studio, addressS) => {
          val idReg: Option[UUID] = registration.flatMap{reg => reg match {case DBRegistration(_,_,_,_) => reg.id case _ => None} }
          entity2model(clazz, studio, addressS, idReg)
        }
      } // The result is Seq[Clazz] flapMap (works with Clazz) these to Page
    }.flatMap (c3 => totalRows.map (rows => Page(c3, page, offset.toLong, rows.toLong)))
  }

  override def listPersonalizedMy(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = "%", idUser: UUID, startFrom: Timestamp, endAt: Timestamp): Future[Page] = {
    val offset = if (page > 0) pageSize * page else 0

    val action = (for {
      registration <- slickRegistrations.filter(_.idTrainee === idUser)
      clazz <- slickClazzes.filter(_.id === registration.idClazz)
      clazzView <- slickClazzViews
        .sortBy(r => orderBy match {case 1 => r.startFrom case _ => r.startFrom})
        .filter(_.startFrom >= startFrom)
        .filter(_.endAt <= endAt)
        .filter(_.searchMeta.toLowerCase like filter.toLowerCase) if clazzView.id === registration.idClazz
      s <- slickStudios.filter(_.id === clazzView.idStudio)
      a <- slickAddresses.filter(_.id === s.idAddress)
    } yield (clazzView, registration, s, a)).drop(offset).take(pageSize)
    val totalRows = countMy(filter, idUser, startFrom, endAt)


    val result = db.run(action.result)
    result.map { clazz =>
      clazz.map {
        // go through all the DBClazzes and map them to Clazz
        case (clazz, registration, studio, addressS) => {
          entity2model(clazz, studio, addressS, registration.id)
        }
      } // The result is Seq[Clazz] flapMap (works with Clazz) these to Page
    }.flatMap (c3 => totalRows.map (rows => Page(c3, page, offset.toLong, rows.toLong)))
  }

  private def countMy(filter: String, idUser: UUID, startFrom: Timestamp, endAt: Timestamp): Future[Int] = {
    val action = for {
      registration <- slickRegistrations.filter(_.idTrainee === idUser)
      clazz <- slickClazzes.filter(_.id === registration.idClazz)
      clazzView <- slickClazzViews
        .filter(_.startFrom >= startFrom)
        .filter(_.endAt <= endAt)
        .filter(_.startFrom >= new Timestamp(System.currentTimeMillis()))
        .filter(_.searchMeta.toLowerCase like filter.toLowerCase) if clazzView.id === registration.idClazz
    } yield registration
    db.run(action.length.result)
  }
}
