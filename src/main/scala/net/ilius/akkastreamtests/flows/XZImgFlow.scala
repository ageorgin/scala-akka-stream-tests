package net.ilius.akkastreamtests.flows

import java.io.InputStream

import akka.NotUsed
import akka.stream.scaladsl.Flow
import net.ilius.akkastreamtests.messages.{PhotoXzimg, PhotoBinary}
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scalaj.http.{MultiPart, HttpResponse, Http}
import scala.concurrent.duration._

/**
  * Created by ageorgin on 01/06/16.
  */
object XZImgFlow {
  private def detectFace(photo: PhotoBinary): Future[PhotoXzimg] = {
    Future.successful {
      println("call to detect face " + photo.aboId)
      val response: HttpResponse[String] = Http("http://192.168.56.146:8080/reverse")
        .postMulti(MultiPart("fullImage", "fullImage.jpg", "image/jpeg", convertToByteArray(photo.photo.getBinaryStream)),
          MultiPart("thumbImage", "thumbImage.jpg", "image/jpeg", convertToByteArray(photo.thumb.getBinaryStream())))
        .timeout(10000, 10000)
        .asString

      response.is2xx match {
        case true => {
          println("XZImg answered")
          PhotoXzimg(photo.aboId, photo.phoId, response.body)
        }
        case _ => throw new RuntimeException("XZImg error")
      }
    }
  }

  private def convertToByteArray(is: InputStream): Array[Byte] = {
    val stream:Stream[Byte] = Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte)
    stream.toArray
  }

  def buildFaceDetectionFlow(): Flow[PhotoBinary, PhotoXzimg, NotUsed] = {
    Flow[PhotoBinary].mapAsyncUnordered(parallelism = 10) {
      photo =>
        detectFace(photo)
    }
  }
}
