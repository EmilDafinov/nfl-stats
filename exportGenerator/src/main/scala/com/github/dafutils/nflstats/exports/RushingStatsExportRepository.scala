package com.github.dafutils.nflstats.exports

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.github.dafutils.nflstats
import com.github.dafutils.nflstats.Selection
import com.github.dafutils.nflstats.db.Queries._
import com.github.dafutils.nflstats.db.Tables.profile.api._
import slick.basic.DatabaseConfig
import slick.jdbc.MySQLProfile

class RushingStatsExportRepository(config: DatabaseConfig[MySQLProfile]) {

  def stream(selection: Selection): Source[nflstats.db.Tables.RushingStatsRow, NotUsed] = {
    Source.fromPublisher(
      config.db.stream(
        selectRushingRecords(selection)
          .result
          .withStatementParameters(
            fetchSize = Integer.MIN_VALUE,
            rsType = slick.jdbc.ResultSetType.ForwardOnly,
            rsConcurrency = slick.jdbc.ResultSetConcurrency.ReadOnly
          )
      )
    )
  }
}
