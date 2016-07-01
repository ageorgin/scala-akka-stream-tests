package net.ilius.akkastreamtests.sources

import akka.NotUsed
import akka.stream.scaladsl.Source
import net.ilius.akkastreamtests.entities.{PhotoAlbumTableDef}
import net.ilius.akkastreamtests.messages.{PhotoAlbum}
import slick.backend.DatabasePublisher
import slick.driver.MySQLDriver.api._
import slick.driver.MySQLDriver.backend.DatabaseDef

/**
  * Created by ageorgin on 27/05/16.
  */
object PhotoAlbumSource {
  def enableStream (statement: java.sql.Statement): Unit = {
    statement match {
      case s: com.mysql.jdbc.StatementImpl => {
        s.enableStreamingResults()
      }
      case _ => // nothing to do
    }
  }

  def buildSource(): Source[PhotoAlbum, NotUsed] = {
    val db = Database.forConfig("mysqlSource")
    val photoAlbumTable: TableQuery[PhotoAlbumTableDef] = TableQuery[PhotoAlbumTableDef]
    val q = for (e <- photoAlbumTable) yield (e.aboId, e.phoId)
    val p1: DatabasePublisher[(String, String)] = db.stream(q.result.withStatementParameters(statementInit = enableStream).withStatementParameters(fetchSize = 100))
    val p2: DatabasePublisher[PhotoAlbum] = p1.mapResult { data =>
      PhotoAlbum(data._1, data._2)
    }
    Source.fromPublisher(p2)
  }

  def buildSource2(db: DatabaseDef): Source[(String, String), NotUsed] = {
    val photoAlbumTable: TableQuery[PhotoAlbumTableDef] = TableQuery[PhotoAlbumTableDef]
    val q = for (e <- photoAlbumTable) yield (e.aboId, e.phoId)
    val p1: DatabasePublisher[(String, String)] = db.stream(q.result.withStatementParameters(statementInit = enableStream).withStatementParameters(fetchSize = 100))
    Source.fromPublisher(p1)
  }
}
