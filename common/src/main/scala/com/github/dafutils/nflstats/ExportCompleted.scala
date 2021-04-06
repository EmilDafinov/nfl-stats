package com.github.dafutils.nflstats

import com.github.dafutils.nflstats.db.ExportStatus.ExportStatus

import java.util.UUID

case class ExportCompleted(exportUuid: UUID, status: ExportStatus)
