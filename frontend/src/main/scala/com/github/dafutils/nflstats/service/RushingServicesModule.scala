package com.github.dafutils.nflstats.service

import akka.stream.alpakka.amqp.AmqpWriteSettings
import akka.stream.alpakka.amqp.scaladsl.AmqpFlow
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.github.dafutils.nflstats.configuration.ConfigurationModule
import com.github.dafutils.nflstats.db.{ExportsRepository, RushingStatsRepository}
import com.github.dafutils.nflstats.json.JsonSupport._
import com.github.dafutils.nflstats.rabbitmq.RabbitMqModule
import com.github.dafutils.nflstats.{ExportCompleted, RequestsSource}
import slick.basic.DatabaseConfig
import slick.jdbc.MySQLProfile

trait AbstractRushingServicesModule {
  val rushingStatsRepository: RushingStatsRepository

  val exportsRepository: ExportsRepository

  val exportDownloadService: ExportDownloadService 

  val exportRequestService: ExportRequestService

  val exportStatusUpdater: ExportStatusUpdater
}

trait RushingServicesModule extends AbstractRushingServicesModule {

  this: ConfigurationModule with RabbitMqModule =>

  private lazy val dbConfig = DatabaseConfig.forConfig[MySQLProfile]("nfl")

  private lazy val s3Config = config.getConfig("s3")

  override lazy val rushingStatsRepository = new RushingStatsRepository(dbConfig)

  override lazy val exportsRepository = new ExportsRepository(dbConfig)

  override lazy val exportDownloadService = new ExportDownloadService(
    exportsRepository = exportsRepository,
    exportBucketName = s3Config.getString("export.bucket.name"),
    s3Client = AmazonS3ClientBuilder
      .standard()
      .withPathStyleAccessEnabled(s3Config.getBoolean("path-style-access"))
      .withEndpointConfiguration(
        new EndpointConfiguration(
          s3Config.getString("endpoint-url"),
          s3Config.getString("aws.region")
        )
      )
      .withCredentials(
        new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(
            s3Config.getString("aws.credentials.access-key-id"),
            s3Config.getString("aws.credentials.secret-access-key")
          )
        )
      )
      .build()
  )

  override lazy val exportRequestService = new ExportRequestService(
    requestPublishingFlowWithConfirmation = AmqpFlow
      .withConfirm(
        AmqpWriteSettings(
          connectionProvider = connectionProvider
        )
          .withExchange(rabbitmqConfig.getString("export.exchange"))
          .withRoutingKey(rabbitmqConfig.getString("export.request.routing.key"))
      )
      .map(_.confirmed),
    exportsRepository = exportsRepository
  )


  override lazy val exportStatusUpdater = new ExportStatusUpdater(
    exportStatusUpdatesSource = RequestsSource[ExportCompleted](
      connectionProvider = connectionProvider,
      requestQueueName = config.getString("rabbitmq.export.completed.queue")
    ),
    exportsRepository = exportsRepository
  )
}
