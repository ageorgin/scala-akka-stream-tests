package net.ilius.akkastreamtests.flows

import java.io.InputStream
import java.sql.Blob

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow}
import net.ilius.akkastreamtests.messages.{PhotoWithCoordinate, PhotoXzimg, PhotoBinary}
import net.ilius.akkastreamtests.xzimg.XZimgResponse
import scala.concurrent.{Future}
import spray.json._
import net.ilius.akkastreamtests.xzimg.XZImgResponseJsonProtocol._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

/**
  * Created by ageorgin on 01/06/16.
  */
object XZImgFlow {
  private def createEntity(photoBinary: PhotoBinary): Future[RequestEntity] = {
    val multipart = Multipart.FormData(
    parts =
    Multipart.FormData.BodyPart("fullImage", HttpEntity(MediaTypes.`image/jpeg`, convertToByteArray(photoBinary.photo.getBinaryStream)), Map("filename" -> "fullImage.jpg")),
    Multipart.FormData.BodyPart("thumbImage", HttpEntity(MediaTypes.`image/jpeg`, convertToByteArray(photoBinary.thumb.getBinaryStream)), Map("filename" -> "thumbImage.jpg"))
    )

    Marshal(multipart).to[RequestEntity]
  }

  private def createRequest(urlXzimgServer: String, entity: RequestEntity, system:ActorSystem, materializer: ActorMaterializer): Future[HttpResponse] = {
    println("Call XZImg reverse API")
    Http(system).singleRequest(HttpRequest(method = HttpMethods.POST, uri = urlXzimgServer, entity = entity))(materializer)
  }

  private def convertToByteArray(is: InputStream): Array[Byte] = {
    val stream:Stream[Byte] = Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte)
    stream.toArray
  }

  private def decodeXzimgJson(photoXzimg: PhotoXzimg): Future[Try[PhotoWithCoordinate]] = {
    println("Decoding JSON for " + photoXzimg.phoId)
    val xzimgResponse = photoXzimg.json.parseJson.convertTo[XZimgResponse]

    xzimgResponse.confidence match {
      case 1 =>
        Future.successful {
          println("Decoding JSON OK for " + photoXzimg.phoId)
          Success(PhotoWithCoordinate(
            photoXzimg.aboId,
            photoXzimg.phoId,
            xzimgResponse.location.x.toString,
            xzimgResponse.location.y.toString,
            xzimgResponse.location.width.toString,
            xzimgResponse.location.height.toString
          ))
        }
      case _ =>
        Future.successful {
          println("Decoding JSON KO for " + photoXzimg.phoId)
          Failure(new RuntimeException("erreur XZimg"))
        }
    }
  }

  def buildFaceDetectionFlow(urlXzimgServer: String, system: ActorSystem, materializer: ActorMaterializer, parallelism: Int): Flow[Try[PhotoBinary], Try[PhotoXzimg], NotUsed] = {
    implicit val mat = materializer

    Flow[Try[PhotoBinary]].mapAsyncUnordered(parallelism = parallelism) {
      case Success(photo) =>
        try {
          println("Face detection for " + photo.phoId)
          val result = for {
            entity <- createEntity(photo)
            futureResponse <- createRequest(urlXzimgServer, entity, system, materializer)
            response <- Unmarshal(futureResponse.entity).to[String]
          } yield response

          result.map(
            response =>
              Success(PhotoXzimg(photo.aboId, photo.phoId, response))
          ) recover { case e =>
            println("Recover erreur XZImg reverse API")
            Failure(e)
          }
        } catch {
          case ex =>
            println("Exception XZImg reverse API")
            Future.successful(
              Failure(ex)
            )
        }

      case Failure(f) =>
        Future.successful {
          println("No face detection")
          Failure(f)
        }
    }
  }

  def buildXzimgJsonDecoderFlow(parallelism: Int): Flow[Try[PhotoXzimg], Try[PhotoWithCoordinate], NotUsed] = {
    Flow[Try[PhotoXzimg]].mapAsyncUnordered(parallelism = parallelism) {
      case Success(photo: PhotoXzimg) =>
        decodeXzimgJson(photo)
      case Failure(f) =>
        Future.successful {
          println("No JSON decoding")
          Failure(f)
        }
    }
  }

}
