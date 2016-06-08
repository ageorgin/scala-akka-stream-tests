package net.ilius.akkastreamtests.sinks

import akka.stream.scaladsl.Sink
import net.ilius.akkastreamtests.entities.PhotoAlbumTableDef
import net.ilius.akkastreamtests.messages.PhotoWithCoordinate
import slick.driver.MySQLDriver.api._
import slick.driver.MySQLDriver.backend.DatabaseDef

/**
  * Created by ageorgin on 07/06/16.
  */
object PhotoAlbumSink {
  private def updatePhotoSource(db: DatabaseDef, photo: PhotoWithCoordinate) = {
    val photoAlbumTable: TableQuery[PhotoAlbumTableDef] = TableQuery[PhotoAlbumTableDef]
    val q = for {
      p <- photoAlbumTable if p.aboId === photo.aboId && p.phoId === photo.phoId
    } yield (p.thumbX, p.thumbY, p.thumbWidth, p.thumbHeight)

    val updateAction = q.update(photo.thumbX.toInt, photo.thumbY.toInt, photo.thumbWidth.toInt, photo.thumbHeight.toInt)
    db.run(updateAction)
    println("PhotoAlbum update for aboId=" + photo.aboId + " and phoId=" + photo.phoId)
  }

  def buildUpdatePhotoSink(db: DatabaseDef) = {
    Sink.foreach[PhotoWithCoordinate] {
      photo =>
        updatePhotoSource(db, photo)
    }
  }
}
