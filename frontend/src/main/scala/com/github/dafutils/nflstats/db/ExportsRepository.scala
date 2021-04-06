package com.github.dafutils.nflstats.db

import akka.Done
import com.github.dafutils.nflstats
import com.github.dafutils.nflstats.{Page, db}
import com.github.dafutils.nflstats.db.ExportStatus.{ExportStatus, FAILED, SUCCESSFUL}
import com.github.dafutils.nflstats.db.Tables.{ExportsRow, _}
import slick.basic.DatabaseConfig
import slick.jdbc.MySQLProfile

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class ExportsRepository(config: DatabaseConfig[MySQLProfile]) {

  import config.profile.api._

  val completedExportStatuses = Set(FAILED, SUCCESSFUL)

  def createExport(owner: UUID, exportUuid: UUID, originalRequest: String)(implicit ec: ExecutionContext): Future[nflstats.db.Tables.ExportsRow] = {

    val insertedRow = ExportsRow(
      uuid = exportUuid,
      userUuid = owner,
      fileKey = s"exports/${owner.toString}/export_${exportUuid.toString}.csv",
      originalRequest = originalRequest
    )
    config.db.run(Exports += insertedRow).map(_ => insertedRow)
  }

  def updateExportStatus(exportUuid: UUID, status: ExportStatus)
                        (implicit ec: ExecutionContext): Future[Int] =
    config.db.run(
      Exports
        .filter(_.uuid === exportUuid)
        .map(_.status)
        .update(status)
    )

  def readExportsBelongingTo(ownerUuid: UUID, page: Page): Future[Seq[db.Tables.ExportsRow]] = config.db.run(
    Exports
      .filter(_.userUuid === ownerUuid)
      .sortBy(_.createdOn.desc)
      .drop(page.index * page.size)
      .take(page.size)
      .result
  )

  def readSuccessfulExportKey(exportUuid: UUID, ownerUuid: UUID): Future[Option[String]] = {
    config.db.run(
      Exports
        .filter(_.uuid === exportUuid)
        .filter(_.userUuid === ownerUuid)
        .filter(_.status === SUCCESSFUL)
        .map(_.fileKey)
        .result.headOption
    )
  }
}
