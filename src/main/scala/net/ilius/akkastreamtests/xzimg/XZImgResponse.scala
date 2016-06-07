package net.ilius.akkastreamtests.xzimg

/**
  * Created by ageorgin on 07/06/16.
  */
case class XZimgResponse(
                          patchFound: Boolean,
                          location: XZimgResponseLocation,
                          confidence: Int
                        )

case class XZimgResponseLocation(
                                  x:Int,
                                  y:Int,
                                  width: Int,
                                  height: Int
                                )
