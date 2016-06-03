package net.ilius.akkastreamtests.entities

import java.sql.Blob

import slick.driver.MySQLDriver.api._

/**
  * Created by ageorgin on 27/05/16.
  */
class PhotoTableDef(tag: Tag, tableName: String) extends Table[(String, String, Blob, Blob)](tag, tableName) {
  def aboId = column[String]("ABO_ID")
  def phoId = column[String]("PHO_ID")
  def photo = column[Blob]("PHO_PHOTO")
  def thumb = column[Blob]("PHO_VIGN105")

  override def * = (aboId, phoId, photo, thumb)
}
