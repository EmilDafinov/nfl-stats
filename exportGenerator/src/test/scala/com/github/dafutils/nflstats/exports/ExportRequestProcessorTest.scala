package com.github.dafutils.nflstats.exports

import akka.Done
import akka.actor.ActorSystem
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import akka.util.ByteString
import com.github.dafutils.nflstats.db.ExportStatus.{FAILED, SUCCESSFUL}
import com.github.dafutils.nflstats.json.JsonSupport._
import com.github.dafutils.nflstats.util.UnitTestSpec
import com.github.dafutils.nflstats.{ExportCompleted, ExportRequest, Selection}
import org.mockito.Mockito.{verify, when}

import java.util.UUID
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class ExportRequestProcessorTest extends TestKit(ActorSystem("test-system")) with UnitTestSpec {

  private val mockExportGenerationService: ExportGenerationService = mock[ExportGenerationService]
  implicit val ec = system.dispatcher

  override def afterAll(): Unit = {
    super.afterAll()
    system.terminate()
  }

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = 1 seconds,
    interval = 50 millis
  )

  "ExportRequestProcessor" should {
    "successfully complete if the source fails" in {
      //given
      val messagesSentToSink: ListBuffer[ByteString] = ListBuffer.empty[ByteString]

      val tested = new ExportRequestProcessor(
        exportRequestsSource = Source.fromIterator(() => throw new RuntimeException("The source went kaboom!")),
        exportStatusMessageSink = Sink.foreach(messagesSentToSink += _),
        exportGenerationService = mockExportGenerationService
      )

      //when
      val eventualResult = tested.listenForExports()

      //then
      whenReady(eventualResult) { _ =>
        //we don't care to assert anything, we just want to complete successfully
      }
    }

    "successfully ack an export and mark it as successful if it completes successfully" in {
      //given
      val mockReadResult = mock[CommittableReadResult]
      val testRequest = ExportRequest(
        selection = Selection(),
        ownerUserUuid = UUID.randomUUID(),
        exportUuid = UUID.randomUUID()
      )

      val messagesSentToSink: ListBuffer[ByteString] = ListBuffer.empty[ByteString]

      val tested = new ExportRequestProcessor(
        exportRequestsSource = Source.fromIterator(() => {
          Seq((mockReadResult, testRequest)).iterator
        }),
        exportStatusMessageSink = Sink.foreach(messagesSentToSink += _),
        exportGenerationService = mockExportGenerationService
      )

      when {
        mockExportGenerationService.`export`(testRequest)
      } thenReturn {
        Future.successful(Done)
      }

      when {
        mockReadResult.ack()
      } thenReturn {
        Future.successful(Done)
      }

      val expectedMessage = ByteString(
        serialization.write(
          ExportCompleted(
            exportUuid = testRequest.exportUuid,
            status = SUCCESSFUL
          )
        )
      )

      //when
      val eventualResult = tested.listenForExports()

      //then
      whenReady(eventualResult) { _ =>
        messagesSentToSink should contain theSameElementsAs Seq(expectedMessage)
        verify(mockReadResult).ack(multiple = false)
      }
    }

    "successfully ack an export and mark it as failed if it didn't complete successfully" in {
      //given
      val mockReadResult = mock[CommittableReadResult]
      val testRequest = ExportRequest(
        selection = Selection(),
        ownerUserUuid = UUID.randomUUID(),
        exportUuid = UUID.randomUUID()
      )

      val messagesSentToSink: ListBuffer[ByteString] = ListBuffer.empty[ByteString]

      val tested = new ExportRequestProcessor(
        exportRequestsSource = Source.fromIterator(() => {
          Seq((mockReadResult, testRequest)).iterator
        }),
        exportStatusMessageSink = Sink.foreach(messagesSentToSink += _),
        exportGenerationService = mockExportGenerationService
      )

      when {
        mockExportGenerationService.`export`(testRequest)
      } thenReturn {
        Future.failed(new RuntimeException("Export generation went kaboom"))
      }

      when {
        mockReadResult.ack()
      } thenReturn {
        Future.successful(Done)
      }

      val expectedMessage = ByteString(
        serialization.write(
          ExportCompleted(
            exportUuid = testRequest.exportUuid,
            status = FAILED
          )
        )
      )

      //when
      val eventualResult = tested.listenForExports()

      //then
      whenReady(eventualResult) { _ =>
        messagesSentToSink should contain theSameElementsAs Seq(expectedMessage)
        verify(mockReadResult).ack(multiple = false)
      }
    }
  }

}
