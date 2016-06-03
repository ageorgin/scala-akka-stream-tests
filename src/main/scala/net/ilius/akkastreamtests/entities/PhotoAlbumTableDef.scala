package net.ilius.akkastreamtests.entities

import slick.driver.MySQLDriver.api._

/**
  * Created by ageorgin on 27/05/16.
  */
class PhotoAlbumTableDef(tag: Tag) extends Table[(String, String, Int, Int, Int, Int)](tag, "PHOTO_ALBUM") {
  def aboId = column[String]("ABO_ID")
  def phoId = column[String]("PHO_ID")
  def thumbX = column[Int]("THUMB_X")
  def thumbY = column[Int]("THUMB_Y")
  def thumbWidth = column[Int]("THUMB_WIDTH")
  def thumbHeight = column[Int]("THUMB_HEIGHT")

  override def * = (aboId, phoId, thumbX, thumbY, thumbWidth, thumbHeight)
}
