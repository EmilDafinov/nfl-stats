package com.github.dafutils.nflstats.db

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import akka.testkit.TestKit
import com.github.dafutils.nflstats
import com.github.dafutils.nflstats.SortField.{LNG, TD, YDS}
import com.github.dafutils.nflstats.{Page, PagedSelection, Selection, SortField}
import com.github.dafutils.nflstats.SortOrder.{ASC, DESC}
import com.github.dafutils.nflstats.util.UnitTestSpec
import slick.basic.DatabaseConfig
import slick.jdbc.MySQLProfile

import scala.concurrent.Await

class RushingStatsRepositoryTest extends TestKit(ActorSystem("TestSystem")) with UnitTestSpec {

  implicit val dbConfig: DatabaseConfig[MySQLProfile] = DatabaseConfig.forConfig("nfl")

  //TODO: These unit tests are not suitable for a CI/CD build as is: they are not ensuring that the flyway migrations are applied
  val tested = new RushingStatsRepository(dbConfig)

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

  val playerNamesForRushingYardsTests = Seq(
    "Joe Banyard", "Robert Turbin", "Corey Grant", "Tyreek Hill"
  )

  val playerNamesForTotalRushingTouchdownsTests = Seq(
    "Lance Dunbar", "Jay Ajayi", "Ryan Mathews", "LeSean McCoy"
  )

  "RushingStatsRepository" should {

    "correctly sort the requested players by rushingYards ascending" in {
      //given
      val expectedRecordNames = Seq(
        "Joe Banyard", "Robert Turbin", "Corey Grant", "Tyreek Hill"
      )

      //when
      val eventualResult = tested.list(
        PagedSelection(
          selection = Selection(
            requestedPlayers = playerNamesForRushingYardsTests,
            maybeSortBy = Some(nflstats.SortBy(YDS, ASC)),

          ),
          page = Page(
            index = 0,
            size = 10
          )
        )
      )

      //then
      whenReady(eventualResult) { actualRecords =>
        actualRecords.map(_.player) should contain theSameElementsInOrderAs expectedRecordNames
      }
    }


    "correctly sort the requested players by rushingYards descending" in {
      //given
      val expectedRecordNames = Seq(
        "Tyreek Hill", "Corey Grant", "Robert Turbin", "Joe Banyard",
      )

      //when
      val eventualResult = tested.list(
        PagedSelection(
          selection = Selection(
            requestedPlayers = playerNamesForRushingYardsTests,
            maybeSortBy = Some(nflstats.SortBy(YDS, DESC)),

          ),
          page = Page(
            index = 0,
            size = 10
          )
        )
      )

      //then
      whenReady(eventualResult) { actualRecords =>
        actualRecords.map(_.player) should contain theSameElementsInOrderAs expectedRecordNames
      }
    }

    "correctly sort the requested players by total rushing touchdowns ascending" in {
      //given
      val expectedRecordNames = Seq(
        "Lance Dunbar", "Jay Ajayi", "Ryan Mathews", "LeSean McCoy"
      )

      //when
      val eventualResult = tested.list(
        PagedSelection(
          selection = Selection(
            requestedPlayers = playerNamesForTotalRushingTouchdownsTests,
            maybeSortBy = Some(nflstats.SortBy(TD, ASC)),

          ),
          page = Page(
            index = 0,
            size = 10
          )
        )
      )

      //then
      whenReady(eventualResult) { actualRecords =>
        actualRecords.map(_.player) should contain theSameElementsInOrderAs expectedRecordNames
      }
    }


    "correctly sort the requested players by total rushing touchdowns descending" in {
      //given
      val expectedRecordNames = Seq(
        "LeSean McCoy", "Ryan Mathews", "Jay Ajayi", "Lance Dunbar",
      )

      //when
      val eventualResult = tested.list(
        PagedSelection(
          selection = Selection(
            requestedPlayers = playerNamesForTotalRushingTouchdownsTests,
            maybeSortBy = Some(nflstats.SortBy(TD, DESC)),

          ),
          page = Page(
            index = 0,
            size = 10
          )
        )
      )

      //then
      whenReady(eventualResult) { actualRecords =>
        actualRecords.map(_.player) should contain theSameElementsInOrderAs expectedRecordNames
      }
    }

    "correctly sort the requested players by longest_rush ascending" in {
      //given
      val expectedRecordNames = Seq(
        "Ty Montgomery",
        "Mack Brown",
        "Justin Forsett",
        "Rob Kelley",
        "Mark Ingram",
        "Jalen Richard",
        "Isaiah Crowell"
      )

      //when
      val eventualResult = tested.list(
        PagedSelection(
          selection = Selection(
            requestedPlayers = playersForLongestRushTests,
            maybeSortBy = Some(nflstats.SortBy(LNG, ASC)),

          ),
          page = Page(
            index = 0,
            size = 10
          )
        )
      )

      //then
      whenReady(eventualResult) { actualRecords =>
        actualRecords.map(_.player) should contain theSameElementsInOrderAs expectedRecordNames
      }
    }

    "correctly sort the requested players by longest_rush rush, descending" in {
      //given
      val expectedRecordNames = Seq(
        "Isaiah Crowell",
        "Jalen Richard",
        "Mark Ingram",
        "Rob Kelley",
        "Justin Forsett",
        "Mack Brown",
        "Ty Montgomery",
      )

      //when
      val eventualResult = tested.list(
        PagedSelection(
          selection = Selection(
            requestedPlayers = playersForLongestRushTests,
            maybeSortBy = Some(nflstats.SortBy(LNG, DESC)),

          ),
          page = Page(
            index = 0,
            size = 10
          )
        )
      )

      //then
      whenReady(eventualResult) { actualRecords =>
        actualRecords.map(_.player) should contain theSameElementsInOrderAs expectedRecordNames
      }
    }

    "correctly return the first page of sorted records for the requested players sorted by longest_rush" in {
      //given
      val expectedRecordNames = Seq(
        "Ty Montgomery",
        "Mack Brown",
        "Justin Forsett",
        "Rob Kelley",
        "Mark Ingram",
      )

      //when
      val eventualResult = tested.list(
        PagedSelection(
          selection = Selection(
            requestedPlayers = playersForLongestRushTests,
            maybeSortBy = Some(nflstats.SortBy(LNG, ASC)),

          ),
          page = Page(
            index = 0,
            size = 5
          )
        )
      )

      //then
      whenReady(eventualResult) { actualRecords =>
        actualRecords.map(_.player) should contain theSameElementsInOrderAs expectedRecordNames
      }
    }

    "correctly return the second page of sorted records for the requested players sorted by longest_rush" in {
      //given
      val expectedRecordNames = Seq(
        "Jalen Richard",
        "Isaiah Crowell",
      )

      //when
      val eventualResult = tested.list(
        PagedSelection(
          selection = Selection(
            requestedPlayers = playersForLongestRushTests,
            maybeSortBy = Some(nflstats.SortBy(LNG, ASC)),

          ),
          page = Page(
            index = 1,
            size = 5
          )
        )
      )

      //then
      whenReady(eventualResult) { actualRecords =>
        actualRecords.map(_.player) should contain theSameElementsInOrderAs expectedRecordNames
      }
    }
  }
}
