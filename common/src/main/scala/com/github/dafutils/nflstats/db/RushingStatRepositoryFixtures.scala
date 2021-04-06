package com.github.dafutils.nflstats.db

import com.github.dafutils.nflstats.PlayerStats
import com.github.dafutils.nflstats.db.Tables.RushingStats
import org.json4s.JsonAST.{JInt, JNothing, JString, JValue}
import org.json4s.jackson.JsonMethods.{parse, pretty}
import slick.basic.DatabaseConfig
import slick.jdbc.MySQLProfile

import scala.io.Source.fromInputStream
import scala.util.Try
import com.github.dafutils.nflstats.json.JsonSupport._

object RushingStatRepositoryFixtures {

  private def extractRecordsFrom(resourceFileName: String) = {
    val testData = resource
      .managed(
        this.getClass.getResourceAsStream(resourceFileName)
      )
      .acquireAndGet(fromInputStream(_).mkString)

    val dataFileContentsJson = parse(testData).extract[List[JValue]]
    val ydsCleaned = dataFileContentsJson.map { rawJson =>
      val ydsValue = (rawJson \ "Yds") match {
        case anInt: JInt => anInt
        case JString(s) => JInt(s.filterNot(_ == ',').toInt)
        case JNothing => JInt(0)

      }
      val result = rawJson.replace("Yds" :: Nil, ydsValue)
      Try(result.extract[PlayerStats]).getOrElse(throw new IllegalArgumentException(s"${pretty(result)} failed to parse"))
    }

    ydsCleaned.map(_.toRecord)
  }

  def setup()(implicit config: DatabaseConfig[MySQLProfile]) = {
    import config.profile.api._

    RushingStats ++= extractRecordsFrom("/rushing.json")
  }

  def teardown()(implicit config: DatabaseConfig[MySQLProfile]) = {
    import config.profile.api._

    RushingStats.delete
  }
}
