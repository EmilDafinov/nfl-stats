package com.github.dafutils.nflstats.service

import com.amazonaws.services.s3.AmazonS3
import com.github.dafutils.nflstats.db.ExportsRepository

import java.net.URL
import java.time.Instant
import java.util.{Date, UUID}
import scala.concurrent.{ExecutionContext, Future}

class ExportDownloadService(exportsRepository: ExportsRepository,
                            exportBucketName: String,
                            s3Client: AmazonS3) {

  private val presignTimeoutSeconds = 30 //Could be made configurable 

  def downloadExport(exportUuid: UUID, ownerUuid: UUID)(implicit ex: ExecutionContext): Future[Option[URL]] = {

    exportsRepository
      .readSuccessfulExportKey(exportUuid = exportUuid, ownerUuid = ownerUuid)
      .map {
        _.map { fileKey =>
          s3Client
            .generatePresignedUrl(
              exportBucketName,
              fileKey,
              Date.from(Instant.now().plusSeconds(presignTimeoutSeconds))
            )
        }
      }
  }
}
