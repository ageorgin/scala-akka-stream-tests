package net.ilius.akkastreamtests.flows

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.typesafe.config.Config
import net.ilius.akkastreamtests.entities.PhotoTableDef
import net.ilius.akkastreamtests.messages.{PhotoBinary, PhotoAlbum}
import slick.driver.MySQLDriver.api._
import slick.driver.MySQLDriver.backend.DatabaseDef
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by ageorgin on 27/05/16.
  */
object PhotoFlow {
  /**
    * Use Object mapping to find row in PhotoTableDef according to aboId and phoId
    *
    * @param db
    * @param aboId
    * @param phoId
    * @return Future[String]
    */
  private def findPhotoBinary(db: DatabaseDef,config: Config, aboId: String, phoId: String): Future[PhotoBinary] = {

    val tableName: String = config.getBoolean("shardingActive") match {
      case true => "PHOTO".concat(phoId.substring(0, 2))
      case false => "PHOTO"
    }
    println("lecture en base")
    val photoTable = TableQuery[PhotoTableDef]((tag: Tag) => new PhotoTableDef(tag, tableName))
    val result = db.run(photoTable.filter(_.aboId === aboId).filter(_.phoId === phoId).result.head)
    result.map( s => {
      println("lecture en base OK")
      PhotoBinary(s._1, s._2, s._3, s._4)
      }
    )
  }

  /**
    * Build flow to process binary photo from database
    * @param db
    * @return
    */
  def buildFlow(db: DatabaseDef, config: Config): Flow[PhotoAlbum, PhotoBinary, NotUsed] = {
    Flow[PhotoAlbum].mapAsyncUnordered(parallelism = config.getInt("parallelism")) {
      photo =>
        findPhotoBinary(db, config, photo.aboId, photo.phoId)
    }
  }
}