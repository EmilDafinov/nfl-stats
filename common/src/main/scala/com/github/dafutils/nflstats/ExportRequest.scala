package com.github.dafutils.nflstats

import java.util.UUID

case class ExportRequest(
  selection: Selection, 
  ownerUserUuid: UUID,
  exportUuid: UUID = UUID.randomUUID()
)
