package net.ilius.akkastreamtests

import java.io.{InputStream, FileOutputStream, BufferedOutputStream, File}
import java.util.concurrent.ExecutorService

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.typesafe.config.ConfigFactory
import net.ilius.akkastreamtests.entities.PhotoTableDef
import net.ilius.akkastreamtests.flows.{XZImgFlow, PhotoFlow}
import net.ilius.akkastreamtests.messages.{PhotoXzimg, PhotoBinary, PhotoAlbum}
import net.ilius.akkastreamtests.sources.{PhotoAlbumSource}
import slick.backend.DatabasePublisher


import slick.driver.MySQLDriver.api._
import scala.concurrent
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
  * Created by ageorgin on 20/05/16.
  */
object Main extends App {
  override def main(args: Array[String]) {

    implicit val system = ActorSystem("test")
    implicit val materializer = ActorMaterializer()

    /*
    // Exemple très simple
    val source = Source(1 to 10)
    val flow:Flow[Int, Int, NotUsed] = Flow[Int].map(_ + 0)
    val sink = Sink.foreach[Int](println(_))

    val stream = source.via(flow).toMat(sink)(Keep.right)

    stream.run()
    */

    // Exemple avec source base de données
    val dbSource = Database.forConfig("mysqlSource")
    val dbPhoto = Database.forConfig("mysqlPhoto")
    val config = ConfigFactory.load().getConfig("app")

    println("config = " + config.getString("xzimgUrl") )

    // La source
    val source: Source[PhotoAlbum, NotUsed] = PhotoAlbumSource.buildSource(dbSource)

    // flow recuperation photo
    val flowPhoto = PhotoFlow.buildFlow(dbPhoto, config)

    // flow detection visage
    val flowDetectFace = XZImgFlow.buildFaceDetectionFlow(config.getString("xzimgUrl"), system, materializer)

    // Sink de debug
    /*val sink = Sink.foreach[Future[PhotoBinary]](
      photoBinary => {
        val result = Await.result(photoBinary, 10 seconds)
        println(result.phoId)

        // pour écrire le fichier sur disque
        //writeToFile(result.phoId.asInstanceOf[String], result.photo.getBinaryStream)
      }
    )*/

    val sink = Sink.foreach[PhotoXzimg] {
      detectResult =>
        println(detectResult.aboId + " " + detectResult.phoId + " " + detectResult.json)
    }

    def writeToFile(filename: String, is: InputStream) = {
      val file:File = new File("/home/ageorgin/_dev/tmp/" + filename + ".jpg")
      val target = new BufferedOutputStream(new FileOutputStream(file))
      val stream:Stream[Byte] = Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte)
      try stream.foreach( target.write(_) ) finally target.close;
      is.reset()
    }

    val stream = source.via(flowPhoto).via(flowDetectFace).async.to(sink);
    stream.run()
  }
}
