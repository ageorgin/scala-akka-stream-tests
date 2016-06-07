package net.ilius.akkastreamtests.xzimg

import spray.json.DefaultJsonProtocol

/**
  * Created by ageorgin on 07/06/16.
  */
object XZImgResponseJsonProtocol extends DefaultJsonProtocol {
  implicit val xzimgResponseLocationFormat = jsonFormat4(XZimgResponseLocation)
  implicit val xzimgResponseFormat = jsonFormat3(XZimgResponse)
}
