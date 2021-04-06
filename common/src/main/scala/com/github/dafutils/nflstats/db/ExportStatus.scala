package com.github.dafutils.nflstats.db

object ExportStatus extends Enumeration {
  type ExportStatus = Value
  val IN_PROGRESS, SUCCESSFUL, FAILED  = Value
}
