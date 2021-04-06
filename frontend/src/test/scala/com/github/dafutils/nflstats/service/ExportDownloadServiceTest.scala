package com.github.dafutils.nflstats.service

import com.amazonaws.services.s3.AmazonS3
import com.github.dafutils.nflstats.db.ExportsRepository
import com.github.dafutils.nflstats.util.UnitTestSpec
import org.mockito.Mockito.when

import java.util.{Date, UUID}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.mockito.ArgumentMatchers.{any, eq => mockEq}

import java.net.URL

class ExportDownloadServiceTest extends UnitTestSpec {

  private val mockRepository: ExportsRepository = mock[ExportsRepository]
  private val testS3BucketName = "testBucketName"
  private val mockS3Client: AmazonS3 = mock[AmazonS3]
  
  val tested = new ExportDownloadService(mockRepository, testS3BucketName, mockS3Client)
  
  "ExportDownloadService" should {
    "return a presigned url" in {
      //given
      val testExportUuid = UUID.fromString("6bbd9885-87f0-49ee-9b70-2e3ed23d6b7b")
      val testOwnerUuid = UUID.fromString("29bbf7b6-68f6-4263-97bc-c7054b86bd06")
      val testS3Key = "test/s3/key"
      val expectedPresignedUrl = new URL("http://example.com")
      
      when {
        mockRepository.readSuccessfulExportKey(exportUuid = testExportUuid, ownerUuid = testOwnerUuid)
      } thenReturn {
        Future.successful(Some(testS3Key))
      }
      
      when {
        mockS3Client.generatePresignedUrl(mockEq(testS3BucketName), mockEq(testS3Key), any[Date])
      } thenReturn {
        expectedPresignedUrl
      }
      
      //when
      val eventualResult = tested.downloadExport(exportUuid = testExportUuid, ownerUuid = testOwnerUuid)
      
      //then
      whenReady(eventualResult) { actualPresignedUrl => 
        actualPresignedUrl shouldEqual Some(expectedPresignedUrl)
      }
      
    }
  }
}
