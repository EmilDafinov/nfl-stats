package com.github.dafutils.nflstats.http

import akka.http.scaladsl.model.StatusCodes.{BadRequest, Found, NotFound, OK}
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.{Location, RawHeader}
import akka.http.scaladsl.server.Route.seal
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.stream.Materializer
import com.github.dafutils.nflstats._
import com.github.dafutils.nflstats.db.Tables.{ExportsRow, RushingStatsRow}
import com.github.dafutils.nflstats.db.{ExportStatus, ExportsRepository, RushingStatsRepository}
import com.github.dafutils.nflstats.http.graphql.GraphqlSchemaModule
import com.github.dafutils.nflstats.json.JsonSupport._
import com.github.dafutils.nflstats.service.{AbstractRushingServicesModule, ExportDownloadService, ExportRequestService, ExportStatusUpdater}
import com.github.dafutils.nflstats.util.UnitTestSpec
import org.json4s.JValue
import org.json4s.jackson.JsonMethods.parse
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => mockEq}
import org.mockito.Mockito.when

import java.net.URL
import java.sql.Timestamp
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source.fromInputStream

class FrontendRouteModuleTest extends UnitTestSpec with ScalatestRouteTest {

  private val tested = new AkkaDependenciesModule with AbstractRushingServicesModule with GraphqlSchemaModule with FrontendRouteModule {
    override val rushingStatsRepository: RushingStatsRepository = mock[RushingStatsRepository]
    override val exportsRepository: ExportsRepository = mock[ExportsRepository]
    override val exportDownloadService: ExportDownloadService = mock[ExportDownloadService]
    override val exportRequestService: ExportRequestService = mock[ExportRequestService]
    override val exportStatusUpdater: ExportStatusUpdater = mock[ExportStatusUpdater]
  }

  implicit val timeout = RouteTestTimeout(scaled(1.seconds))

  val testCallingUserUuid = UUID.fromString("b88e85ad-f1cb-4e14-860e-5eb4a94267a3")
  val testExportUuid = UUID.fromString("5ca2e256-2b06-4b11-8d01-d53c8600d109")

  "HttpRoute" should {
    "execute a rushing stats request and get the first page" in {
      //given
      val testedQuery = resource
        .managed(
          getClass.getResourceAsStream("/test-payloads/rushingStatsRequest.json")
        )
        .acquireAndGet(fromInputStream(_).mkString)

      val expectedResponse = resource
        .managed(
          getClass.getResourceAsStream("/test-payloads/rushingStatsExpectedResponse.json")
        )
        .acquireAndGet(fromInputStream(_).mkString)

      when {
        tested.rushingStatsRepository.list(
          pagedSelection = PagedSelection(
            selection = Selection(
              requestedPlayers = Seq("A Player"),
              maybeSortBy = Some(
                SortBy(
                  sortField = SortField.LNG,
                  sortOrder = SortOrder.DESC
                )
              )
            ),
            page = Page(
              index = 0,
              size = 5
            )
          )
        )
      } thenReturn {
        Future.successful(
          Seq(
            RushingStatsRow(id = 1, player = "APlayer", position = "front", team = "ScaryAnimals", rushingAttempts = 1, rushingAttemptsPerGame = 12.34, rushingYards = 5, rushingAverageYardsPerAttempt = 23.45, rushingYardsPerGame = 34.45, totalRushingTouchdowns = 1, longestRush = 2, touchdownOccurred = true, rushingFirstDowns = 3, rushingFirstDownsPercentage = 12.00, rushing20PlusYards = 2, rushing40PlusYards = 6, rushingFumbles = 8),
            RushingStatsRow(id = 2, player = "APlayer2", position = "back", team = "NonScaryAnimals", rushingAttempts = 2, rushingAttemptsPerGame = 13.34, rushingYards = 6, rushingAverageYardsPerAttempt = 24.45, rushingYardsPerGame = 35.45, totalRushingTouchdowns = 2, longestRush = 3, touchdownOccurred = false, rushingFirstDowns = 4, rushingFirstDownsPercentage = 13.00, rushing20PlusYards = 3, rushing40PlusYards = 7, rushingFumbles = 9),
          )
        )
      }

      //when
      Post(uri = "/nfl/graphql", content = parse(testedQuery)) ~>
        RawHeader("user_uuid", testCallingUserUuid.toString) ~>
        seal(tested.applicationRoute) ~>
        check {
          //then
          response.status shouldEqual OK
          val actual = entityAs[JValue]
          actual shouldEqual parse(expectedResponse)
        }
    }

    "fail to execute a rushing stats request when the user uuid header is missing" in {
      //given
      val testedQuery = resource
        .managed(
          getClass.getResourceAsStream("/test-payloads/rushingStatsRequest.json")
        )
        .acquireAndGet(fromInputStream(_).mkString)

      //when
      Post(uri = "/nfl/graphql", content = parse(testedQuery)) ~>
        seal(tested.applicationRoute) ~>
        check {
          //then
          response.status shouldEqual BadRequest
        }
    }
    
    "fail to execute a rushing stats request when the requested page is more than the maximum" in {
      //given
      val testedQuery = resource
        .managed(
          getClass.getResourceAsStream("/test-payloads/rushingStatsRequestPageTooLarge.json")
        )
        .acquireAndGet(fromInputStream(_).mkString)

      val expectedResponse = resource
        .managed(
          getClass.getResourceAsStream("/test-payloads/rushingStatsRequestPageTooLargeExpectedResponse.json")
        )
        .acquireAndGet(fromInputStream(_).mkString)

      //when
      Post(uri = "/nfl/graphql", content = parse(testedQuery)) ~>
        RawHeader("user_uuid", testCallingUserUuid.toString) ~>
        seal(tested.applicationRoute) ~>
        check {
          //then
          val actual = entityAs[JValue]
          actual shouldEqual parse(expectedResponse)
        }
    }

    "list the first page of exports belonging to the user calling the API" in {
      //given
      val testedQuery = resource
        .managed(
          getClass.getResourceAsStream("/test-payloads/exportsRequest.json")
        )
        .acquireAndGet(fromInputStream(_).mkString)

      val expectedResponse = resource
        .managed(
          getClass.getResourceAsStream("/test-payloads/exportsRequestExpectedResponse.json")
        )
        .acquireAndGet(fromInputStream(_).mkString)

      when {
        tested.exportsRepository.readExportsBelongingTo(
          ownerUuid = testCallingUserUuid,
          page = Page(
            index = 0,
            size = 5
          )
        )
      } thenReturn {
        Future.successful(
          Seq(
            ExportsRow(
              uuid = UUID.fromString("65f4b3a6-9979-4a75-ac00-690328d41a62"),
              userUuid = testCallingUserUuid,
              createdOn = new Timestamp(100000),
              fileKey = "a/file/key",
              status = ExportStatus.SUCCESSFUL,
              originalRequest = """{"dummy" : "json"}}"""
            ),
            ExportsRow(
              uuid = UUID.fromString("65f4b3a6-9979-4a75-ac00-690328d41a62"),
              userUuid = testCallingUserUuid,
              createdOn = new Timestamp(200000),
              fileKey = "another/file/key",
              status = ExportStatus.FAILED,
              originalRequest = """{"dummy2" : "json2"}}"""
            ),
            ExportsRow(
              uuid = UUID.fromString("65f4b3a6-9979-4a75-ac00-690328d41a62"),
              userUuid = testCallingUserUuid,
              createdOn = new Timestamp(300000),
              fileKey = "a/third/file/key",
              status = ExportStatus.IN_PROGRESS,
              originalRequest = """{"dummy3" : "json3"}}"""
            )
          )
        )
      }

      //when
      Post(uri = "/nfl/graphql", content = parse(testedQuery)) ~>
        RawHeader("user_uuid", s"$testCallingUserUuid") ~>
        seal(tested.applicationRoute) ~>
        check {
          //then
          response.status shouldEqual OK
          val actual = entityAs[JValue]
          actual shouldEqual parse(expectedResponse)
        }
    }

    "fail to list the first page of exports when the user uuid header is missing" in {
      //given
      val testedQuery = resource
        .managed(
          getClass.getResourceAsStream("/test-payloads/exportsRequest.json")
        )
        .acquireAndGet(fromInputStream(_).mkString)

      //when
      Post(uri = "/nfl/graphql", content = parse(testedQuery)) ~>
        seal(tested.applicationRoute) ~>
        check {
          //then
          response.status shouldEqual BadRequest
        }
    }

    "fail listing exports if the requested page is too large" in {
      //given
      val testedQuery = resource
        .managed(
          getClass.getResourceAsStream("/test-payloads/exportsRequestPageTooLarge.json")
        )
        .acquireAndGet(fromInputStream(_).mkString)

      val expectedResponse = resource
        .managed(
          getClass.getResourceAsStream("/test-payloads/exportsRequestPageTooLargeExpectectedResponse.json")
        )
        .acquireAndGet(fromInputStream(_).mkString)

      //when
      Post(uri = "/nfl/graphql", content = parse(testedQuery)) ~>
        RawHeader("user_uuid", s"$testCallingUserUuid") ~>
        seal(tested.applicationRoute) ~>
        check {
          //then
          val actual = entityAs[JValue]
          actual shouldEqual parse(expectedResponse)
        }
    }

    "trigger an export" in {
      //given
      val testedQuery = resource
        .managed(
          getClass.getResourceAsStream("/test-payloads/triggerExportRequest.json")
        )
        .acquireAndGet(fromInputStream(_).mkString)

      val expectedResponse = resource
        .managed(
          getClass.getResourceAsStream("/test-payloads/triggerExportRequestExpectedResponse.json")
        )
        .acquireAndGet(fromInputStream(_).mkString)

      val requestCaptor: ArgumentCaptor[ExportRequest] = ArgumentCaptor.forClass(classOf[ExportRequest])

      val expectedExportSelection = Selection(
        requestedPlayers = Seq("A Player"),
        maybeSortBy = Some(
          SortBy(
            sortField = SortField.LNG,
            sortOrder = SortOrder.DESC
          )
        )
      )

      when {
        tested.exportRequestService.requestExport(
          exportRequest = requestCaptor.capture()
        )(any[ExecutionContext], any[Materializer])
      } thenReturn {
        Future.successful(
          UUID.fromString("aeb892ac-bf16-465b-aff7-dde3ac675944")
        )
      }

      //when
      Post(uri = "/nfl/graphql", content = parse(testedQuery)) ~>
        RawHeader("user_uuid", s"$testCallingUserUuid") ~>
        seal(tested.applicationRoute) ~>
        check {
          //then
          response.status shouldEqual OK
          val actualResponse = entityAs[JValue]
          val actualExportRequest = requestCaptor.getValue

          actualResponse shouldEqual parse(expectedResponse)
          actualExportRequest.ownerUserUuid shouldEqual testCallingUserUuid
          actualExportRequest.selection shouldEqual expectedExportSelection
        }
    }

    "fail triggering an export when the user uuid header is missing" in {
      //given
      val testedQuery = resource
        .managed(
          getClass.getResourceAsStream("/test-payloads/triggerExportRequest.json")
        )
        .acquireAndGet(fromInputStream(_).mkString)

      //when
      Post(uri = "/nfl/graphql", content = parse(testedQuery)) ~>
        seal(tested.applicationRoute) ~>
        check {
          //then
          response.status shouldEqual BadRequest
        }
    }
    
    "download a export" in {
      //given
      val expectedRedirectUrl = new URL("http://example.com")

      when {
        tested.exportDownloadService
          .downloadExport(
            exportUuid = mockEq(testExportUuid),
            ownerUuid = mockEq(testCallingUserUuid)
          )(any[ExecutionContext])
      } thenReturn {
        Future.successful(Some(expectedRedirectUrl))
      }

      //when
      Get(uri = s"/nfl/exports/$testExportUuid") ~>
        RawHeader("user_uuid", testCallingUserUuid.toString) ~>
        seal(tested.applicationRoute) ~>
        check {
          //then
          response.status shouldEqual Found
          response.headers should contain theSameElementsAs Location(Uri(expectedRedirectUrl.toString)) :: Nil
        }
    }

    "fail downloading a export when a user uuid header is not found" in {

      //when
      Get(uri = s"/nfl/exports/$testExportUuid") ~>
        seal(tested.applicationRoute) ~>
        check {
          //then
          response.status shouldEqual BadRequest
        }
    }
    
    "fail downloading a export when export not found for the requested user" in {
      //given
      when {
        tested.exportDownloadService
          .downloadExport(
            exportUuid = mockEq(testExportUuid),
            ownerUuid = mockEq(testCallingUserUuid)
          )(any[ExecutionContext])
      } thenReturn {
        Future.successful(None)
      }

      //when
      Get(uri = s"/nfl/exports/$testExportUuid") ~>
        RawHeader("user_uuid", testCallingUserUuid.toString) ~>
        seal(tested.applicationRoute) ~>
        check {
          //then
          response.status shouldEqual NotFound
        }
    }
  }
}
