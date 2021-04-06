package com.github.dafutils.nflstats.http

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

trait ExporterHttpRouteModule extends ApplicationHttpRoute {
  val applicationRoute: Route = (pathPrefix("health") & get) {
    complete(HttpResponse())
  }
}
