package com.github.dafutils.nflstats.http

import akka.http.scaladsl.server.Route

trait ApplicationHttpRoute {
  val applicationRoute: Route
}
