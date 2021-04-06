package com.github.dafutils.nflstats

import com.github.dafutils.nflstats.db.Tables

case class PlayerStats(Player: String,
                       Team: String,
                       Pos: String,
                       Att: Int,
                       `Att/G`: Double,
                       Yds: Int,
                       Avg: Double,
                       `Yds/G`: Double,
                       TD: Int,
                       Lng: String, //Can be a number or T indicating touchdown, adjust the type
                       `1st`: Int,
                       `1st%`: Double,
                       `20+`: Int,
                       `40+`: Int,
                       FUM: Int,
                      ) {
  def toRecord: Tables.RushingStatsRow = Tables.RushingStatsRow(
    id = 0,
    player = Player,
    team = Team,
    position = Pos,
    rushingAttempts = Att,
    rushingAttemptsPerGame = `Att/G`,
    rushingYards = Yds,
    rushingAverageYardsPerAttempt = Avg,
    rushingYardsPerGame = `Yds/G`,
    totalRushingTouchdowns = TD,
    longestRush = Lng.filterNot(_ == 'T') match {
      case str: String if str.isEmpty => 0
      case nonEmptyStr => nonEmptyStr.toInt
    },
    touchdownOccurred = Lng.contains("T"),
    rushingFirstDowns = `1st`,
    rushingFirstDownsPercentage = `1st%`,
    rushing20PlusYards = `20+`,
    rushing40PlusYards = `40+`,
    rushingFumbles = FUM
  )
}
