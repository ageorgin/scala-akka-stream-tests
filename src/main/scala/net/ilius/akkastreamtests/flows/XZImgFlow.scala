package net.ilius.akkastreamtests.flows

import java.io.InputStream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow}
import net.ilius.akkastreamtests.messages.{PhotoXzimg, PhotoBinary}
import scala.concurrent.{Future}
import scala.concurrent.ExecutionContext.Implicits.global

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

  def buildFaceDetectionFlow(urlXzimgServer: String, system: ActorSystem, materializer: ActorMaterializer): Flow[PhotoBinary, PhotoXzimg, NotUsed] = {
    implicit val mat = materializer

    Flow[PhotoBinary].mapAsyncUnordered(parallelism = 10) {
      photo =>
        val result = for {
          entity <- createEntity(photo)
          futureResponse <- createRequest(urlXzimgServer, entity, system, materializer)
          response <- Unmarshal(futureResponse.entity).to[String]
        } yield response

        result.map(
          response =>
            PhotoXzimg(photo.aboId, photo.phoId, response)
        )
    }
  }


}
