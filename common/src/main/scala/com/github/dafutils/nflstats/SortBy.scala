package com.github.dafutils.nflstats

import com.github.dafutils.nflstats.SortField.SortField
import com.github.dafutils.nflstats.SortOrder.{ASC, SortOrder}

case class SortBy(sortField: SortField, sortOrder: SortOrder = ASC)
