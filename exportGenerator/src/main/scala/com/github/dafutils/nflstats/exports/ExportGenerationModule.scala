package com.github.dafutils.nflstats.exports

import akka.Done
import akka.stream.alpakka.amqp.AmqpWriteSettings
import akka.stream.alpakka.amqp.scaladsl.AmqpSink
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.{ActorAttributes, Supervision}
import com.github.dafutils.nflstats.configuration.ConfigurationModule
import com.github.dafutils.nflstats.json.JsonSupport._
import com.github.dafutils.nflstats.rabbitmq.RabbitMqModule
import com.github.dafutils.nflstats.{AkkaDependenciesModule, ExportRequest, RequestsSource}
import slick.basic.DatabaseConfig
import slick.jdbc.MySQLProfile

import scala.concurrent.Future
import scala.util.control.NonFatal

trait ExportGenerationModule {
  this: ConfigurationModule with AkkaDependenciesModule with RabbitMqModule =>
  lazy val dbConfig = DatabaseConfig.forConfig[MySQLProfile]("nfl")

  lazy val rushingStatsRepository = new RushingStatsExportRepository(dbConfig)

  lazy val exportRequestsSource = RequestsSource[ExportRequest](
    connectionProvider = connectionProvider,
    requestQueueName = rabbitmqConfig.getString("export.requests.queue")
  )

  lazy val exportGenerationService = new ExportGenerationService(
    rushingStatsRepository = rushingStatsRepository,
    sinkGenerator = exportKey =>
      S3
        .multipartUpload(
          bucket = config.getString("export.bucket.name"),
          key = exportKey
        )
        .mapMaterializedValue(_ => Future.successful(Done))
        .withAttributes(
          ActorAttributes.supervisionStrategy {
            case NonFatal(_) => Supervision.stop
          }
        )
  )

  lazy val exportRequestProcessor = new ExportRequestProcessor(
    exportRequestsSource = exportRequestsSource,
    exportGenerationService = exportGenerationService,
    exportStatusMessageSink = AmqpSink
      .simple(
        AmqpWriteSettings(connectionProvider)
          .withExchange(rabbitmqConfig.getString("export.exchange"))
          .withRoutingKey(rabbitmqConfig.getString("export.completed.routing.key"))
      )
  )
}
