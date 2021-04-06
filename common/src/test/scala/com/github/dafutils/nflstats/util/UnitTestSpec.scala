package com.github.dafutils.nflstats.util

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar

trait UnitTestSpec
  extends AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with MockitoSugar
    with SpanSugar
    with BeforeAndAfterEach
    with BeforeAndAfterAll
