package com.github.dafutils.nflstats.http

import akka.http.scaladsl.Http
import com.github.dafutils.nflstats.AkkaDependenciesModule
import com.github.dafutils.nflstats.configuration.ConfigurationModule
import com.typesafe.scalalogging.Logger

import scala.concurrent.Future
import scala.util.control.NonFatal

trait HttpServerModule {
  this: ConfigurationModule with AkkaDependenciesModule with ApplicationHttpRoute =>

  private val interface = config.getString("http.interface")
  private val port = config.getInt("http.port")
  private val log = Logger(this.getClass)

  def startHttpServer(): Future[Http.ServerBinding] = {
    val result = Http()
      .newServerAt(interface = interface, port = port)
      .bind(applicationRoute)
      .recover {
        case NonFatal(ex) =>
          log.error("Http binding failed", ex)
          throw ex
      }
    result.foreach(_ => log.info(s"Started HTTP server at $interface port $port"))

    result
  }
}
