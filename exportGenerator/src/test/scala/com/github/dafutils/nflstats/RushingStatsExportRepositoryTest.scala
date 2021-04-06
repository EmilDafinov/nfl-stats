package com.github.dafutils.nflstats

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import akka.testkit.TestKit
import com.github.dafutils.nflstats
import com.github.dafutils.nflstats.db.RushingStatRepositoryFixtures
import com.github.dafutils.nflstats.exports.RushingStatsExportRepository
import com.github.dafutils.nflstats.util.UnitTestSpec
import slick.basic.DatabaseConfig
import slick.jdbc.MySQLProfile

import scala.concurrent.Await

class RushingStatsExportRepositoryTest extends TestKit(ActorSystem("TestSystem")) with UnitTestSpec {

  implicit val dbConfig: DatabaseConfig[MySQLProfile] = DatabaseConfig.forConfig("nfl")

  //TODO: These unit tests are not suitable for a CI/CD build as is: they are not ensuring that the flyway migrations are applied
  val tested = new RushingStatsExportRepository(dbConfig)

  override def beforeAll(): Unit = {
    super.beforeAll()
    Await.result(
      awaitable = dbConfig.db.run(RushingStatRepositoryFixtures.setup()),
      atMost = 1 minute
    )
  }

  override def afterAll(): Unit = {
    super.afterAll()
    Await.result(
      awaitable = dbConfig.db.run(RushingStatRepositoryFixtures.teardown()),
      atMost = 1 minute
    )
    system.terminate()
  }

  val playersForLongestRushTests = Seq(
    "Rob Kelley", "Jalen Richard", "Ty Montgomery", "Justin Forsett", "Mack Brown", "Mark Ingram", "Isaiah Crowell"
  )

  "RushingStatsExportRepository" should {

    "stream all longest rush data" in {
      //given
      val playersSource = tested.stream(
        selection = Selection(
          requestedPlayers = playersForLongestRushTests,
          maybeSortBy = Some(
            nflstats.SortBy(
              sortField = SortField.LNG,
            )
          )
        )
      )

      val expectedPlayerNames = Seq(
        "Ty Montgomery",
        "Mack Brown",
        "Justin Forsett",
        "Rob Kelley",
        "Mark Ingram",
        "Jalen Richard",
        "Isaiah Crowell",
      )

      //when
      val eventualResult = playersSource.runWith(Sink.seq)

      //then
      whenReady(eventualResult) { actualPlayerRows =>
        actualPlayerRows.map(_.player) should contain theSameElementsInOrderAs expectedPlayerNames
      }
    }
  }
}
