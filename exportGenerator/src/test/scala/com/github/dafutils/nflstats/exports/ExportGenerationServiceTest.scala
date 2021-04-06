package com.github.dafutils.nflstats.exports

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import akka.util.ByteString
import com.github.dafutils.nflstats.db.Tables.RushingStatsRow
import com.github.dafutils.nflstats.{ExportRequest, Selection}
import com.github.dafutils.nflstats.util.UnitTestSpec
import org.mockito.Mockito.when

import java.util.UUID
import scala.collection.mutable.ListBuffer

class ExportGenerationServiceTest extends TestKit(ActorSystem("test-system")) with UnitTestSpec {

  implicit val ec = system.dispatcher
  val messagesSentToSink = ListBuffer.empty[ByteString]
  private val mockRepository: RushingStatsExportRepository = mock[RushingStatsExportRepository]
  val tested = new ExportGenerationService(
    rushingStatsRepository = mockRepository, 
    sinkGenerator = key => Sink.foreach(messagesSentToSink += _)
  )

  override def afterEach(): Unit = {
    super.afterEach()
    messagesSentToSink.clear()
  }
  
  override def afterAll(): Unit = {
    super.afterAll()
    system.terminate()
  }

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = 1 seconds,
    interval = 50 millis
  )
  
  "ExportGenerationService" should {
    "fail if the export fails" in {
      //given
      val testRequest = ExportRequest(
        selection = Selection(),
        ownerUserUuid = UUID.randomUUID(),
        exportUuid = UUID.randomUUID()
      )
      
      val expectedErrorMessage = "KABOOM"
      
      when {
        mockRepository.stream(testRequest.selection)
      } thenReturn {
        Source.failed(new RuntimeException(expectedErrorMessage))
      }
      
      //when
      val eventualResult = tested.`export`(exportRequest = testRequest)
      
      //then
      whenReady(eventualResult.failed) { actualErrors => 
        actualErrors.getMessage shouldEqual expectedErrorMessage
      }
    }

    "successfully transform incoming stats to CSV rows and send them to the output" in {
      //given
      val testRequest = ExportRequest(
        selection = Selection(),
        ownerUserUuid = UUID.randomUUID(),
        exportUuid = UUID.randomUUID()
      )
      
      when {
        mockRepository.stream(testRequest.selection)
      } thenReturn {
        Source.fromIterator( () => 
          Seq(
            RushingStatsRow(id = 1, player = "APlayer", position = "front", team = "ScaryAnimals", rushingAttempts = 1, rushingAttemptsPerGame = 12.34, rushingYards = 5, rushingAverageYardsPerAttempt = 23.45, rushingYardsPerGame = 34.45, totalRushingTouchdowns = 1, longestRush = 2, touchdownOccurred = true, rushingFirstDowns = 3, rushingFirstDownsPercentage = 12.00, rushing20PlusYards = 2, rushing40PlusYards = 6, rushingFumbles = 8),
            RushingStatsRow(id = 2, player = "APlayer2", position = "back", team = "NonScaryAnimals", rushingAttempts = 2, rushingAttemptsPerGame = 13.34, rushingYards = 6, rushingAverageYardsPerAttempt = 24.45, rushingYardsPerGame = 35.45, totalRushingTouchdowns = 2, longestRush = 3, touchdownOccurred = false, rushingFirstDowns = 4, rushingFirstDownsPercentage = 13.00, rushing20PlusYards = 3, rushing40PlusYards = 7, rushingFumbles = 9),
          ).iterator
        )
      }
      
      val expectedMessages = Seq(
        ByteString(
          "APlayer,ScaryAnimals,front,1,12.34,5,23.45,34.45,1,2,true,3,12.0,2,6,8\n"
        ),
        ByteString(
          "APlayer2,NonScaryAnimals,back,2,13.34,6,24.45,35.45,2,3,false,4,13.0,3,7,9\n"
        ),
      )
      
      //when
      val eventualResult = tested.`export`(exportRequest = testRequest)

      //then
      whenReady(eventualResult) { _ => 
        messagesSentToSink should contain theSameElementsInOrderAs expectedMessages
        
      }
    }
  }
}
