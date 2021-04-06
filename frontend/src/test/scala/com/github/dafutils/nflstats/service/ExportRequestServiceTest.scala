package com.github.dafutils.nflstats.service

import akka.Done
import akka.actor.ActorSystem
import akka.stream.alpakka.amqp.WriteMessage
import akka.stream.scaladsl.Flow
import akka.testkit.TestKit
import com.github.dafutils.nflstats.SortField.LNG
import com.github.dafutils.nflstats.SortOrder.DESC
import com.github.dafutils.nflstats.db.ExportsRepository
import com.github.dafutils.nflstats.db.Tables.ExportsRow
import com.github.dafutils.nflstats.json.JsonSupport._
import com.github.dafutils.nflstats.util.UnitTestSpec
import com.github.dafutils.nflstats.{ExportRequest, Selection, SortBy}
import org.mockito.Mockito.when

import java.sql.Timestamp
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ExportRequestServiceTest extends TestKit(ActorSystem("TestSystem")) with UnitTestSpec {

  val mockExportsRepository: ExportsRepository = mock[ExportsRepository]

  override def afterAll(): Unit = {
    super.afterAll()
    system.terminate()
  }

  "ExportRequestService" should {

    "successfully trigger an export" in {
      //given
      val flowThatEnqueuesSuccessfully = Flow[WriteMessage]
        .map(_ => true)
        .mapMaterializedValue(_ => Future.successful(Done))

      val tested = new ExportRequestService(flowThatEnqueuesSuccessfully, mockExportsRepository)

      val testOwnerUuid = UUID.fromString("691e755d-4bf4-490d-a312-4d9925288ada")
      val testExportRequest = ExportRequest(
        selection = Selection(
          requestedPlayers = Seq("A Player"),
          maybeSortBy = Some(
            SortBy(
              sortField = LNG,
              sortOrder = DESC
            )
          )
        ),
        ownerUserUuid = testOwnerUuid,
      )
      val testFileKey = "an/s3/key"

      when {
        mockExportsRepository.createExport(
          owner = testOwnerUuid,
          exportUuid = testExportRequest.exportUuid,
          originalRequest = serialization.write(testExportRequest)
        )
      } thenReturn {
        Future.successful(
          ExportsRow(
            uuid = testExportRequest.exportUuid,
            userUuid = testOwnerUuid,
            createdOn = new Timestamp(System.currentTimeMillis()),
            fileKey = testFileKey,
            originalRequest = serialization.write(testExportRequest)
          )
        )
      }

      //when
      val eventualResult = tested.requestExport(
        testExportRequest
      )

      //then
      whenReady(eventualResult) { actualExportUuid =>
        actualExportUuid shouldEqual testExportRequest.exportUuid
      }
    }

    "throw if the export request is not confirmed to be enqueued" in {
      //given
      val flowThatDoesNotEnqueue = Flow[WriteMessage]
        .map(_ => false)
        .mapMaterializedValue(_ => Future.successful(Done))

      val tested = new ExportRequestService(flowThatDoesNotEnqueue, mockExportsRepository)

      val testOwnerUuid = UUID.fromString("691e755d-4bf4-490d-a312-4d9925288ada")
      val testExportRequest = ExportRequest(
        selection = Selection(
          requestedPlayers = Seq("A Player"),
          maybeSortBy = Some(
            SortBy(
              sortField = LNG,
              sortOrder = DESC
            )
          )
        ),
        ownerUserUuid = testOwnerUuid,
      )
      val testFileKey = "an/s3/key"

      when {
        mockExportsRepository.createExport(
          owner = testOwnerUuid,
          exportUuid = testExportRequest.exportUuid,
          originalRequest = serialization.write(testExportRequest)
        )
      } thenReturn {
        Future.successful(
          ExportsRow(
            uuid = testExportRequest.exportUuid,
            userUuid = testOwnerUuid,
            createdOn = new Timestamp(System.currentTimeMillis()),
            fileKey = testFileKey,
            originalRequest = serialization.write(testExportRequest)
          )
        )
      }

      //when
      val eventualResult = tested.requestExport(
        testExportRequest
      )

      //then
      whenReady(eventualResult.failed) { ex =>
        ex.getMessage shouldEqual "Failed to enqueue export request"
      }
    }
  }
}
