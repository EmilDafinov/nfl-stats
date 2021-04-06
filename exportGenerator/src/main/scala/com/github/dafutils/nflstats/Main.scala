package com.github.dafutils.nflstats

import com.github.dafutils.nflstats.configuration.ConfigurationModule
import com.github.dafutils.nflstats.exports.ExportGenerationModule
import com.github.dafutils.nflstats.http.{ExporterHttpRouteModule, HttpServerModule}
import com.github.dafutils.nflstats.rabbitmq.RabbitMqModule
import com.typesafe.scalalogging.Logger

import scala.util.Failure

object Main extends App
  with ConfigurationModule
  with AkkaDependenciesModule
  with RabbitMqModule
  with ExportGenerationModule
  with ExporterHttpRouteModule
  with HttpServerModule {

  private val logger = Logger(this.getClass)
  startHttpServer()
    .andThen {
      case Failure(exception) =>
        logger.error("Http server startup failed", exception)
        System.exit(1)
    }
  exportRequestProcessor
    .listenForExports()
    .andThen {
      case Failure(exception) =>
        logger.error("stopped listening for incoming export requests, terminating", exception)
        System.exit(1)
    }
}
