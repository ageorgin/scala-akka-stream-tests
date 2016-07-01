package net.ilius.akkastreamtests.sinks

import akka.stream.scaladsl.Sink
import net.ilius.akkastreamtests.entities.PhotoAlbumTableDef
import net.ilius.akkastreamtests.messages.PhotoWithCoordinate
import slick.driver.MySQLDriver.api._
import slick.driver.MySQLDriver.backend.DatabaseDef

import scala.util.{Failure, Success, Try}

/**
  * Created by ageorgin on 07/06/16.
  */
object PhotoAlbumSink {
  private def updatePhotoSource(photo: PhotoWithCoordinate) = {
    val db = Database.forConfig("mysqlSource")
    val photoAlbumTable: TableQuery[PhotoAlbumTableDef] = TableQuery[PhotoAlbumTableDef]
    val q = for {
      p <- photoAlbumTable if p.aboId === photo.aboId && p.phoId === photo.phoId
    } yield (p.thumbX, p.thumbY, p.thumbWidth, p.thumbHeight)

    val updateAction = q.update(photo.thumbX.toInt, photo.thumbY.toInt, photo.thumbWidth.toInt, photo.thumbHeight.toInt)
    try db.run(updateAction)
    finally db.close()
    println("PhotoAlbum updated for aboId=" + photo.aboId + " and phoId=" + photo.phoId)
  }

  private def faceDetected(photo: PhotoWithCoordinate): Boolean = {
    photo.thumbX != null && photo.thumbY != null && photo.thumbWidth != null && photo.thumbHeight != null
  }

  def buildUpdatePhotoSink() = {
    Sink.foreach[Try[PhotoWithCoordinate]] {
      case Success(photo: PhotoWithCoordinate) =>
        faceDetected(photo) match {
          case true => updatePhotoSource(photo)
          case false => println("PhotoAlbum not updated for aboId=" + photo.aboId + " and phoId=" + photo.phoId)
        }
      case Failure(f) =>
        println("PhotoAlbum not updated")
    }
  }
}
