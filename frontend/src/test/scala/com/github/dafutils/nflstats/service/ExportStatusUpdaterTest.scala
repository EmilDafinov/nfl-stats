package com.github.dafutils.nflstats.service

import akka.actor.ActorSystem
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import com.github.dafutils.nflstats.ExportCompleted
import com.github.dafutils.nflstats.db.{ExportStatus, ExportsRepository}
import com.github.dafutils.nflstats.util.UnitTestSpec
import org.mockito.Mockito.{verify, when}

import java.util.UUID
import scala.concurrent.Future

class ExportStatusUpdaterTest extends TestKit(ActorSystem("TestSystem")) with UnitTestSpec {
  implicit val ec = system.dispatcher
  val mockExportRepository = mock[ExportsRepository]

  override def afterAll(): Unit = {
    super.afterAll()
    system.terminate()
  }

  "ExportStatusUpdater" should {
    "complete successfully if the source fails" in {
      //given
      val testRequestSource = Source.fromIterator(() => throw new RuntimeException("The source went kaboom!"))
      val tested = new ExportStatusUpdater(
        exportStatusUpdatesSource = testRequestSource,
        exportsRepository = mockExportRepository
      )

      //when
      val eventualResult = tested.listenForExportUpdates()

      //then
      whenReady(eventualResult) { _ =>
        //no need to assert, we only care that we completed successfully
      }
    }

    "complete successfully and nack message if updating a request status failed" in {
      //given
      val mockRabbitMqMessage = mock[CommittableReadResult]
      val testExportCompletedRequest = ExportCompleted(
        exportUuid = UUID.fromString("d262d3b3-c576-4c40-9849-edcb6559b8ab"), 
        status = ExportStatus.SUCCESSFUL
      )
      val testRequestSource = Source.single((mockRabbitMqMessage, testExportCompletedRequest))
      val tested = new ExportStatusUpdater(
        exportStatusUpdatesSource = testRequestSource,
        exportsRepository = mockExportRepository
      )

      when {
        mockExportRepository.updateExportStatus(
          exportUuid = testExportCompletedRequest.exportUuid,
          status = testExportCompletedRequest.status
        )
      } thenReturn {
        Future.failed(new RuntimeException("Kaboom"))
      }
      
      //when
      val eventualResult = tested.listenForExportUpdates()

      //then
      whenReady(eventualResult) { _ =>
        verify(mockRabbitMqMessage).nack(requeue = false)
      }
    }
    
  }

}
