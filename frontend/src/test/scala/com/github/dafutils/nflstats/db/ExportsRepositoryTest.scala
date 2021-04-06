package com.github.dafutils.nflstats.db

import akka.Done
import com.github.dafutils.nflstats.Page
import com.github.dafutils.nflstats.db.ExportStatus.{IN_PROGRESS, SUCCESSFUL}
import com.github.dafutils.nflstats.db.Tables.Exports
import com.github.dafutils.nflstats.util.UnitTestSpec
import slick.basic.DatabaseConfig
import slick.jdbc.MySQLProfile

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class ExportsRepositoryTest extends UnitTestSpec {

  val dbConfig: DatabaseConfig[MySQLProfile] = DatabaseConfig.forConfig("nfl")

  val tested = new ExportsRepository(dbConfig)

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = 5 seconds
  )

  override def afterEach(): Unit = {
    super.afterAll()
    import dbConfig.profile.api._
    Await.result(
      awaitable = dbConfig.db.run(Exports.delete),
      atMost = 1 minute
    )
  }

  val testUserUuid = UUID.fromString("8d2af59c-8548-4b44-bb1a-8fcd58e22cae")
  val testExportUuid = UUID.fromString("e089c811-579a-4904-9523-7638e2e7f9c2")
  val testOriginalRequest = """{"a": "dummy"}"""

  "ExportsRepository" should {
    "create an export for a user and return it on request" in {

      //given
      val expectedStatus = IN_PROGRESS
      val expectedDeviation = 1 minute

      //when 
      val eventualResult = for {
        _ <- tested.createExport(
          owner = testUserUuid,
          exportUuid = testExportUuid,
          originalRequest = testOriginalRequest
        )
        exportsRead <- tested.readExportsBelongingTo(
          ownerUuid = testUserUuid,
          page = Page(
            index = 0,
            size = 5
          )
        )
      } yield exportsRead

      //then
      whenReady(eventualResult) { actualExports =>
        actualExports should have size 1
        val expectedFileKey = s"exports/${testUserUuid.toString}/export_${actualExports.head.uuid.toString}.csv"
        actualExports.head.userUuid shouldEqual testUserUuid
        actualExports.head.fileKey shouldEqual expectedFileKey
        actualExports.head.status shouldEqual expectedStatus
        (System.currentTimeMillis() - actualExports.head.createdOn.getTime) < expectedDeviation.toMillis
      }
    }

    "complete an existing export and update its status to the one requested" in {

      //given
      val expectedStatus = SUCCESSFUL
      val expectedDeviation = 1 minute

      //when 
      val eventualResult = for {
        exportInserted <- tested.createExport(
          owner = testUserUuid,
          exportUuid = testExportUuid,
          originalRequest = testOriginalRequest
        )
        _ <- tested.updateExportStatus(
          exportUuid = exportInserted.uuid,
          status = SUCCESSFUL
        )
        exportsRead <- tested.readExportsBelongingTo(
          ownerUuid = testUserUuid,
          page = Page(
            index = 0,
            size = 10
          )
        )
      } yield exportsRead

      //then
      whenReady(eventualResult) { actualExports =>
        actualExports should have size 1
        val expectedFileKey = s"exports/${testUserUuid.toString}/export_${actualExports.head.uuid.toString}.csv"
        actualExports.head.userUuid shouldEqual testUserUuid
        actualExports.head.fileKey shouldEqual expectedFileKey
        actualExports.head.status shouldEqual expectedStatus
        (System.currentTimeMillis() - actualExports.head.createdOn.getTime) < expectedDeviation.toMillis
      }
    }
    
    "read the file key of a completed successful export" in {
      //given
      val expectedExportFileKey = s"exports/${testUserUuid.toString}/export_${testExportUuid.toString}.csv"
      
      //when
      val eventualResult = for {
        _ <- tested.createExport(
          owner = testUserUuid, 
          exportUuid = testExportUuid, 
          originalRequest = ""
        )
        _ <- tested.updateExportStatus(
          exportUuid = testExportUuid,
          status = SUCCESSFUL
        )
        key <- tested.readSuccessfulExportKey(
          exportUuid = testExportUuid, 
          ownerUuid = testUserUuid
        )
      } yield key
      
      //then
      whenReady(eventualResult) { actualKey =>
        actualKey shouldEqual Some(expectedExportFileKey)
      }
    }

    "fail to read the file key of an export that is not yet successful" in {

      //when
      val eventualResult = for {
        _ <- tested.createExport(
          owner = testUserUuid,
          exportUuid = testExportUuid,
          originalRequest = ""
        )
        key <- tested.readSuccessfulExportKey(
          exportUuid = testExportUuid,
          ownerUuid = testUserUuid
        )
      } yield key

      //then
      whenReady(eventualResult) { actualKey =>
        actualKey shouldBe 'empty
      }
    }
    
    "return empty on a non-existing export key" in {
      //when
      val eventualResult = tested.readSuccessfulExportKey(
          exportUuid = testExportUuid,
          ownerUuid = testUserUuid
        )
      

      //then
      whenReady(eventualResult) { actualKey =>
        actualKey shouldBe 'empty
      }
    }
  }
}
