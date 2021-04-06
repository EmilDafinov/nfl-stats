package com.github.dafutils.nflstats.db

import com.github.dafutils.nflstats.{Selection, SortBy, db}
import com.github.dafutils.nflstats.db.Tables.RushingStats
import slick.lifted.Query
import com.github.dafutils.nflstats.SortField._
import com.github.dafutils.nflstats.SortOrder._

object Queries {
  def selectRushingRecords(selection: Selection): Query[db.Tables.RushingStats, db.Tables.RushingStatsRow, Seq] = {
    import com.github.dafutils.nflstats.db.Tables.profile.api._

    val filteredRecords = RushingStats
      .filter(_.player.inSet(selection.requestedPlayers) || selection.requestedPlayers.isEmpty)

    //Note: the record db id is added to each sorting in order to guarantee predictable
    //      ordering of records that have the same value of the column we are sorting on
    selection.maybeSortBy match {
      case None => filteredRecords
        
      case Some(SortBy(YDS, ASC)) => filteredRecords.sortBy(rushingStats =>
        (rushingStats.rushingYards.asc, rushingStats.id.asc)
      )
      case Some(SortBy(YDS, DESC)) => filteredRecords.sortBy(rushingStats =>
        (rushingStats.rushingYards.desc, rushingStats.id.desc)
      )
      case Some(SortBy(TD, ASC)) => filteredRecords.sortBy(rushingStats =>
        (rushingStats.totalRushingTouchdowns.asc, rushingStats.id.asc)
      )
      case Some(SortBy(TD, DESC)) => filteredRecords.sortBy(rushingStats =>
        (rushingStats.totalRushingTouchdowns.desc, rushingStats.id.desc)
      )
      case Some(SortBy(LNG, ASC)) => filteredRecords.sortBy { rushingStats =>
        (rushingStats.longestRush.asc, rushingStats.touchdownOccurred.asc, rushingStats.id.asc)
      }
      case Some(SortBy(LNG, DESC)) => filteredRecords.sortBy { rushingStats =>
        (rushingStats.longestRush.desc, rushingStats.touchdownOccurred.desc, rushingStats.id.desc)
      }
    }
  }
}
