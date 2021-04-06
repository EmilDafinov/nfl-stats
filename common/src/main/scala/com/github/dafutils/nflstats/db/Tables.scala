package com.github.dafutils.nflstats.db

import com.github.dafutils.nflstats.db.ExportStatus.{ExportStatus, IN_PROGRESS}

import java.sql.Timestamp
import java.util.UUID
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = slick.jdbc.MySQLProfile
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Exports.schema ++ FlywaySchemaHistory.schema ++ RushingStats.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table Exports
   *  @param uuid Database column uuid SqlType(BINARY), PrimaryKey
   *  @param userUuid Database column user_uuid SqlType(BINARY)
   *  @param createdOn Database column created_on SqlType(TIMESTAMP)
   *  @param fileKey Database column file_key SqlType(VARCHAR), Length(255,true)
   *  @param status Database column status SqlType(VARCHAR), Length(16,true)
   *  @param originalRequest Database column original_request SqlType(TEXT) */
  case class ExportsRow(uuid: UUID,
                        userUuid: UUID,
                        createdOn: java.sql.Timestamp = new Timestamp(System.currentTimeMillis()), 
                        fileKey: String, 
                        status: ExportStatus = IN_PROGRESS,
                        originalRequest: String)
  
  /** GetResult implicit for fetching ExportsRow objects using plain SQL queries */
  implicit def GetResultExportsRow(implicit e0: GR[UUID], e1: GR[java.sql.Timestamp], e2: GR[String], e3: GR[ExportStatus]): GR[ExportsRow] = GR{
    prs => import prs._
    ExportsRow.tupled((<<[UUID], <<[UUID], <<[java.sql.Timestamp], <<[String], <<[ExportStatus], <<[String]))
  }
  
  implicit val exportStatusToStringMapper =
    MappedColumnType.base[ExportStatus, String]({ formatType =>
      formatType.toString
    }, { str =>
      ExportStatus.withName(str)
    })
  
  /** Table description of table exports. Objects of this class serve as prototypes for rows in queries. */
  class Exports(_tableTag: Tag) extends profile.api.Table[ExportsRow](_tableTag, Some("nfl"), "exports") {
    def * = (uuid, userUuid, createdOn, fileKey, status, originalRequest) <> (ExportsRow.tupled, ExportsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(uuid), Rep.Some(userUuid), Rep.Some(createdOn), Rep.Some(fileKey), Rep.Some(status), Rep.Some(originalRequest))).shaped.<>({r=>import r._; _1.map(_=> ExportsRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column uuid SqlType(BINARY), PrimaryKey */
    val uuid: Rep[UUID] = column[UUID]("uuid", O.PrimaryKey)
    /** Database column user_uuid SqlType(BINARY) */
    val userUuid: Rep[UUID] = column[UUID]("user_uuid")
    /** Database column created_on SqlType(TIMESTAMP) */
    val createdOn: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("created_on")
    /** Database column file_key SqlType(VARCHAR), Length(255,true) */
    val fileKey: Rep[String] = column[String]("file_key", O.Length(255,varying=true))
    /** Database column status SqlType(VARCHAR), Length(16,true) */
    val status: Rep[ExportStatus] = column[ExportStatus]("status", O.Length(16,varying=true))
    /** Database column original_request SqlType(TEXT) */
    val originalRequest: Rep[String] = column[String]("original_request")
  }
  /** Collection-like TableQuery object for table Exports */
  lazy val Exports = new TableQuery(tag => new Exports(tag))

  /** Entity class storing rows of table FlywaySchemaHistory
   *  @param installedRank Database column installed_rank SqlType(INT), PrimaryKey
   *  @param version Database column version SqlType(VARCHAR), Length(50,true), Default(None)
   *  @param description Database column description SqlType(VARCHAR), Length(200,true)
   *  @param `type` Database column type SqlType(VARCHAR), Length(20,true)
   *  @param script Database column script SqlType(VARCHAR), Length(1000,true)
   *  @param checksum Database column checksum SqlType(INT), Default(None)
   *  @param installedBy Database column installed_by SqlType(VARCHAR), Length(100,true)
   *  @param installedOn Database column installed_on SqlType(TIMESTAMP)
   *  @param executionTime Database column execution_time SqlType(INT)
   *  @param success Database column success SqlType(BIT) */
  case class FlywaySchemaHistoryRow(installedRank: Int, version: Option[String] = None, description: String, `type`: String, script: String, checksum: Option[Int] = None, installedBy: String, installedOn: java.sql.Timestamp, executionTime: Int, success: Boolean)
  /** GetResult implicit for fetching FlywaySchemaHistoryRow objects using plain SQL queries */
  implicit def GetResultFlywaySchemaHistoryRow(implicit e0: GR[Int], e1: GR[Option[String]], e2: GR[String], e3: GR[Option[Int]], e4: GR[java.sql.Timestamp], e5: GR[Boolean]): GR[FlywaySchemaHistoryRow] = GR{
    prs => import prs._
    FlywaySchemaHistoryRow.tupled((<<[Int], <<?[String], <<[String], <<[String], <<[String], <<?[Int], <<[String], <<[java.sql.Timestamp], <<[Int], <<[Boolean]))
  }
  /** Table description of table flyway_schema_history. Objects of this class serve as prototypes for rows in queries.
   *  NOTE: The following names collided with Scala keywords and were escaped: type */
  class FlywaySchemaHistory(_tableTag: Tag) extends profile.api.Table[FlywaySchemaHistoryRow](_tableTag, Some("nfl"), "flyway_schema_history") {
    def * = (installedRank, version, description, `type`, script, checksum, installedBy, installedOn, executionTime, success) <> (FlywaySchemaHistoryRow.tupled, FlywaySchemaHistoryRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(installedRank), version, Rep.Some(description), Rep.Some(`type`), Rep.Some(script), checksum, Rep.Some(installedBy), Rep.Some(installedOn), Rep.Some(executionTime), Rep.Some(success))).shaped.<>({r=>import r._; _1.map(_=> FlywaySchemaHistoryRow.tupled((_1.get, _2, _3.get, _4.get, _5.get, _6, _7.get, _8.get, _9.get, _10.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column installed_rank SqlType(INT), PrimaryKey */
    val installedRank: Rep[Int] = column[Int]("installed_rank", O.PrimaryKey)
    /** Database column version SqlType(VARCHAR), Length(50,true), Default(None) */
    val version: Rep[Option[String]] = column[Option[String]]("version", O.Length(50,varying=true), O.Default(None))
    /** Database column description SqlType(VARCHAR), Length(200,true) */
    val description: Rep[String] = column[String]("description", O.Length(200,varying=true))
    /** Database column type SqlType(VARCHAR), Length(20,true)
     *  NOTE: The name was escaped because it collided with a Scala keyword. */
    val `type`: Rep[String] = column[String]("type", O.Length(20,varying=true))
    /** Database column script SqlType(VARCHAR), Length(1000,true) */
    val script: Rep[String] = column[String]("script", O.Length(1000,varying=true))
    /** Database column checksum SqlType(INT), Default(None) */
    val checksum: Rep[Option[Int]] = column[Option[Int]]("checksum", O.Default(None))
    /** Database column installed_by SqlType(VARCHAR), Length(100,true) */
    val installedBy: Rep[String] = column[String]("installed_by", O.Length(100,varying=true))
    /** Database column installed_on SqlType(TIMESTAMP) */
    val installedOn: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("installed_on")
    /** Database column execution_time SqlType(INT) */
    val executionTime: Rep[Int] = column[Int]("execution_time")
    /** Database column success SqlType(BIT) */
    val success: Rep[Boolean] = column[Boolean]("success")

    /** Index over (success) (database name flyway_schema_history_s_idx) */
    val index1 = index("flyway_schema_history_s_idx", success)
  }
  /** Collection-like TableQuery object for table FlywaySchemaHistory */
  lazy val FlywaySchemaHistory = new TableQuery(tag => new FlywaySchemaHistory(tag))

  /** Entity class storing rows of table RushingStats
   *  @param id Database column id SqlType(INT), AutoInc, PrimaryKey
   *  @param player Database column player SqlType(TEXT)
   *  @param team Database column team SqlType(TEXT)
   *  @param position Database column position SqlType(TEXT)
   *  @param rushingAttempts Database column rushing_attempts SqlType(INT)
   *  @param rushingAttemptsPerGame Database column rushing_attempts_per_game SqlType(DECIMAL)
   *  @param rushingYards Database column rushing_yards SqlType(INT)
   *  @param rushingAverageYardsPerAttempt Database column rushing_average_yards_per_attempt SqlType(DECIMAL)
   *  @param rushingYardsPerGame Database column rushing_yards_per_game SqlType(DECIMAL)
   *  @param totalRushingTouchdowns Database column total_rushing_touchdowns SqlType(INT)
   *  @param longestRush Database column longest_rush SqlType(INT)
   *  @param touchdownOccurred Database column touchdown_occurred SqlType(BIT)
   *  @param rushingFirstDowns Database column rushing_first_downs SqlType(INT)
   *  @param rushingFirstDownsPercentage Database column rushing_first_downs_percentage SqlType(DECIMAL)
   *  @param rushing20PlusYards Database column rushing_20_plus_yards SqlType(INT)
   *  @param rushing40PlusYards Database column rushing_40_plus_yards SqlType(INT)
   *  @param rushingFumbles Database column rushing_fumbles SqlType(INT) */
  case class RushingStatsRow(id: Int, player: String, team: String, position: String, rushingAttempts: Int, rushingAttemptsPerGame: scala.math.BigDecimal, rushingYards: Int, rushingAverageYardsPerAttempt: scala.math.BigDecimal, rushingYardsPerGame: scala.math.BigDecimal, totalRushingTouchdowns: Int, longestRush: Int, touchdownOccurred: Boolean, rushingFirstDowns: Int, rushingFirstDownsPercentage: scala.math.BigDecimal, rushing20PlusYards: Int, rushing40PlusYards: Int, rushingFumbles: Int)
  /** GetResult implicit for fetching RushingStatsRow objects using plain SQL queries */
  implicit def GetResultRushingStatsRow(implicit e0: GR[Int], e1: GR[String], e2: GR[scala.math.BigDecimal], e3: GR[Boolean]): GR[RushingStatsRow] = GR{
    prs => import prs._
    RushingStatsRow.tupled((<<[Int], <<[String], <<[String], <<[String], <<[Int], <<[scala.math.BigDecimal], <<[Int], <<[scala.math.BigDecimal], <<[scala.math.BigDecimal], <<[Int], <<[Int], <<[Boolean], <<[Int], <<[scala.math.BigDecimal], <<[Int], <<[Int], <<[Int]))
  }
  /** Table description of table rushing_stats. Objects of this class serve as prototypes for rows in queries. */
  class RushingStats(_tableTag: Tag) extends profile.api.Table[RushingStatsRow](_tableTag, Some("nfl"), "rushing_stats") {
    def * = (id, player, team, position, rushingAttempts, rushingAttemptsPerGame, rushingYards, rushingAverageYardsPerAttempt, rushingYardsPerGame, totalRushingTouchdowns, longestRush, touchdownOccurred, rushingFirstDowns, rushingFirstDownsPercentage, rushing20PlusYards, rushing40PlusYards, rushingFumbles) <> (RushingStatsRow.tupled, RushingStatsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(player), Rep.Some(team), Rep.Some(position), Rep.Some(rushingAttempts), Rep.Some(rushingAttemptsPerGame), Rep.Some(rushingYards), Rep.Some(rushingAverageYardsPerAttempt), Rep.Some(rushingYardsPerGame), Rep.Some(totalRushingTouchdowns), Rep.Some(longestRush), Rep.Some(touchdownOccurred), Rep.Some(rushingFirstDowns), Rep.Some(rushingFirstDownsPercentage), Rep.Some(rushing20PlusYards), Rep.Some(rushing40PlusYards), Rep.Some(rushingFumbles))).shaped.<>({r=>import r._; _1.map(_=> RushingStatsRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get, _9.get, _10.get, _11.get, _12.get, _13.get, _14.get, _15.get, _16.get, _17.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column player SqlType(TEXT) */
    val player: Rep[String] = column[String]("player")
    /** Database column team SqlType(TEXT) */
    val team: Rep[String] = column[String]("team")
    /** Database column position SqlType(TEXT) */
    val position: Rep[String] = column[String]("position")
    /** Database column rushing_attempts SqlType(INT) */
    val rushingAttempts: Rep[Int] = column[Int]("rushing_attempts")
    /** Database column rushing_attempts_per_game SqlType(DECIMAL) */
    val rushingAttemptsPerGame: Rep[scala.math.BigDecimal] = column[scala.math.BigDecimal]("rushing_attempts_per_game")
    /** Database column rushing_yards SqlType(INT) */
    val rushingYards: Rep[Int] = column[Int]("rushing_yards")
    /** Database column rushing_average_yards_per_attempt SqlType(DECIMAL) */
    val rushingAverageYardsPerAttempt: Rep[scala.math.BigDecimal] = column[scala.math.BigDecimal]("rushing_average_yards_per_attempt")
    /** Database column rushing_yards_per_game SqlType(DECIMAL) */
    val rushingYardsPerGame: Rep[scala.math.BigDecimal] = column[scala.math.BigDecimal]("rushing_yards_per_game")
    /** Database column total_rushing_touchdowns SqlType(INT) */
    val totalRushingTouchdowns: Rep[Int] = column[Int]("total_rushing_touchdowns")
    /** Database column longest_rush SqlType(INT) */
    val longestRush: Rep[Int] = column[Int]("longest_rush")
    /** Database column touchdown_occurred SqlType(BIT) */
    val touchdownOccurred: Rep[Boolean] = column[Boolean]("touchdown_occurred")
    /** Database column rushing_first_downs SqlType(INT) */
    val rushingFirstDowns: Rep[Int] = column[Int]("rushing_first_downs")
    /** Database column rushing_first_downs_percentage SqlType(DECIMAL) */
    val rushingFirstDownsPercentage: Rep[scala.math.BigDecimal] = column[scala.math.BigDecimal]("rushing_first_downs_percentage")
    /** Database column rushing_20_plus_yards SqlType(INT) */
    val rushing20PlusYards: Rep[Int] = column[Int]("rushing_20_plus_yards")
    /** Database column rushing_40_plus_yards SqlType(INT) */
    val rushing40PlusYards: Rep[Int] = column[Int]("rushing_40_plus_yards")
    /** Database column rushing_fumbles SqlType(INT) */
    val rushingFumbles: Rep[Int] = column[Int]("rushing_fumbles")
  }
  /** Collection-like TableQuery object for table RushingStats */
  lazy val RushingStats = new TableQuery(tag => new RushingStats(tag))
}
