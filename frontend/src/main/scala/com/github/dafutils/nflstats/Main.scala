package com.github.dafutils.nflstats

import akka.Done
import com.github.dafutils.nflstats.configuration.ConfigurationModule
import com.github.dafutils.nflstats.db.DatabaseMigrationModule
import com.github.dafutils.nflstats.http.{FrontendRouteModule, HttpServerModule}
import com.github.dafutils.nflstats.http.graphql.GraphqlSchemaModule
import com.github.dafutils.nflstats.rabbitmq.RabbitMqModule
import com.github.dafutils.nflstats.service.RushingServicesModule
import com.typesafe.scalalogging.Logger

import scala.util.Failure

object Main extends App 
  with ConfigurationModule 
  with AkkaDependenciesModule
  with RabbitMqModule
  with RushingServicesModule
  with GraphqlSchemaModule 
  with DatabaseMigrationModule
  with FrontendRouteModule
  with HttpServerModule {
  
  val log = Logger(this.getClass)
  
  exportStatusUpdater
    .listenForExportUpdates()
    .andThen {
      case Failure(exception) =>
        log.error("stopped listening for incoming export requests, terminating", exception)
        // We want to kill the application, to ensure we never end up in a state where we are still running
        // but not listening to export updates from the RabbitMQ queue
        System.exit(1)
    }

  (for {
    _ <- migrateDatabase()
    _ <- startHttpServer()
    
  } yield Done)
    .andThen {
      // Technically we don't need to kill the app here: if the HTTP server doesn't start, k8s 
      // will axe the pod. 
      case Failure(exception) =>
        log.error("Application startup failed", exception)
        System.exit(1)
    }
  
}
