package net.ilius.akkastreamtests.messages

import java.sql.Blob


/**
  * Created by ageorgin on 27/05/16.
  */
case class PhotoAlbum (
                      aboId: String,
                      phoId: String
                      )

case class PhotoBinary (
                         aboId: String,
                         phoId: String,
                         photo: Blob,
                         thumb: Blob
                       )

case class PhotoXzimg (
                        aboId: String,
                        phoId: String,
                        json: String
                      )

case class PhotoWithCoordinate (
                               aboId: String,
                               phoId: String,
                               thumbX: String,
                               thumbY: String,
                               thumbWidth: String,
                               thumbHeight: String
                               )
