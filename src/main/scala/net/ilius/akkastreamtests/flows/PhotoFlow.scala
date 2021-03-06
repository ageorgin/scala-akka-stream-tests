package net.ilius.akkastreamtests.flows

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.mysql.jdbc.Blob
import com.typesafe.config.Config
import net.ilius.akkastreamtests.entities.PhotoTableDef
import net.ilius.akkastreamtests.messages.{PhotoBinary, PhotoAlbum}
import slick.driver.MySQLDriver.api._
import slick.driver.MySQLDriver.backend.DatabaseDef
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Try, Success}

/**
  * Created by ageorgin on 27/05/16.
  */
object PhotoFlow {
  /**
    * Use Object mapping to find row in PhotoTableDef according to aboId and phoId
    *
    * @param aboId
    * @param phoId
    * @return Future[String]
    */
  private def findPhotoBinary(config: Config, aboId: String, phoId: String): Future[Try[PhotoBinary]] = {

    val db = Database.forConfig("mysqlPhoto")
    val tableName: String = config.getBoolean("shardingActive") match {
      case true => "PHOTO".concat(phoId.substring(0, 2))
      case false => "PHOTO"
    }
    println("lecture en base")
    val photoTable = TableQuery[PhotoTableDef]((tag: Tag) => new PhotoTableDef(tag, tableName))
    val result = db.run(photoTable.filter(_.aboId === aboId).filter(_.phoId === phoId).result.head)

    result.map {
      s =>
        println("lecture en base OK " + phoId)
        db.close()
        Success(PhotoBinary(s._1, s._2, s._3, s._4))
    } recover { case e =>
      println("lecture en base KO " + phoId)
      db.close()
      Failure(e)
    }
  }

  /**
    * Build flow to process binary photo from database
    * @return
    */
  def buildFlow(config: Config): Flow[PhotoAlbum, Try[PhotoBinary], NotUsed] = {
    Flow[PhotoAlbum].mapAsyncUnordered(parallelism = config.getInt("parallelism")) {
      photo =>
        findPhotoBinary(config, photo.aboId, photo.phoId)
    }
  }
}