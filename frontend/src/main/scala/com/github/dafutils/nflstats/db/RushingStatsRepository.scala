package com.github.dafutils.nflstats.db

import com.github.dafutils.nflstats.PagedSelection
import com.github.dafutils.nflstats.db.Queries._
import com.github.dafutils.nflstats.db.Tables.profile.api._
import slick.basic.DatabaseConfig
import slick.jdbc.MySQLProfile

import scala.concurrent.Future

class RushingStatsRepository(config: DatabaseConfig[MySQLProfile]) {

  def list(pagedSelection: PagedSelection): Future[Seq[Tables.RushingStatsRow]] = {

    val filteredSortedAndPagedQuery = selectRushingRecords(pagedSelection.selection)
      .drop(pagedSelection.page.index * pagedSelection.page.size)
      .take(pagedSelection.page.size)
      .result

    config.db.run(filteredSortedAndPagedQuery)
  }
}
