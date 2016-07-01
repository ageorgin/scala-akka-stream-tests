package net.ilius.akkastreamtests

import java.io.{InputStream, FileOutputStream, BufferedOutputStream, File}
import java.util.concurrent.ExecutorService

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializerSettings, ActorMaterializer}
import akka.stream.scaladsl._
import com.typesafe.config.ConfigFactory
import net.ilius.akkastreamtests.entities.{PhotoTableDef}
import net.ilius.akkastreamtests.flows.{XZImgFlow, PhotoFlow}
import net.ilius.akkastreamtests.messages.{PhotoWithCoordinate, PhotoXzimg, PhotoBinary, PhotoAlbum}
import net.ilius.akkastreamtests.sinks.{PhotoAlbumSink}
import net.ilius.akkastreamtests.sources.{PhotoAlbumSource}
import net.ilius.akkastreamtests.xzimg.XZimgResponse
import net.ilius.akkastreamtests.xzimg.XZImgResponseJsonProtocol._
import slick.backend.DatabasePublisher


import slick.driver.MySQLDriver.api._
import spray.json._
import DefaultJsonProtocol._
import scala.concurrent
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
  * Created by ageorgin on 20/05/16.
  */
object Main extends App {
  override def main(args: Array[String]) {

    implicit val system = ActorSystem("test")
    implicit val materializer = ActorMaterializer(
      ActorMaterializerSettings(system).withInputBuffer(128, 128)
    )

    /*
    // Exemple très simple
    val source = Source(1 to 10)
    val flow:Flow[Int, Int, NotUsed] = Flow[Int].map(_ + 0)
    val sink = Sink.foreach[Int](println(_))

    val stream = source.via(flow).toMat(sink)(Keep.right)

    stream.run()
    */

    // Exemple avec source base de données
    val config = ConfigFactory.load().getConfig("app")

    println("config = " + config.getString("xzimgUrl") )

    // La source
    val source: Source[PhotoAlbum, NotUsed] = PhotoAlbumSource.buildSource()

    // flow recuperation photo
    val flowPhoto = PhotoFlow.buildFlow(config)

    // flow detection visage
    val flowDetectFace = XZImgFlow.buildFaceDetectionFlow(config.getString("xzimgUrl"), system, materializer, config.getInt("parallelism"))

    // flow décodage JSON
    val flowJsonDecoder = XZImgFlow.buildXzimgJsonDecoderFlow(config.getInt("parallelism"))

    // Sink updgrade db
    val sink = PhotoAlbumSink.buildUpdatePhotoSink()

    def writeToFile(filename: String, is: InputStream) = {
      val file:File = new File("/home/ageorgin/_dev/tmp/" + filename + ".jpg")
      val target = new BufferedOutputStream(new FileOutputStream(file))
      val stream:Stream[Byte] = Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte)
      try stream.foreach( target.write(_) ) finally target.close;
      is.reset()
    }

    val stream = source.via(flowPhoto).via(flowDetectFace).via(flowJsonDecoder).to(sink);
    stream.run()
  }
}
