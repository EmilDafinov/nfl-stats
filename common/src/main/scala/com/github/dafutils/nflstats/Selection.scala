package com.github.dafutils.nflstats

case class Selection(requestedPlayers: Seq[String] = Seq.empty,
                     maybeSortBy: Option[SortBy] = None)
