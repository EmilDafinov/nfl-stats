package com.github.dafutils.nflstats.service

import akka.actor.ActorSystem
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.Supervision
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.stream.scaladsl.{Sink, Source}
import akka.{Done, NotUsed}
import com.github.dafutils.nflstats.ExportCompleted
import com.github.dafutils.nflstats.db.ExportsRepository
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ExportStatusUpdater(exportStatusUpdatesSource: Source[(CommittableReadResult, ExportCompleted), NotUsed],
                          exportsRepository: ExportsRepository) {

  private val log = Logger(this.getClass)
  private val maxParallelRequests = 10 //TODO: We can make this configurable

  def listenForExportUpdates()(implicit system: ActorSystem, ec: ExecutionContext): Future[Done] = {

    exportStatusUpdatesSource
      .mapAsyncUnordered(maxParallelRequests) { case (rabbitMqMessage, parsedRequest) =>
        log.info(s"Received export status update request $parsedRequest")
        exportsRepository
          .updateExportStatus(
            exportUuid = parsedRequest.exportUuid,
            status = parsedRequest.status
          )
          .recover {
            case NonFatal(ex) =>
              log.error("Failed processing export status update request", ex)
              rabbitMqMessage.nack(requeue = false) //By default drops the message. It would send to DLX if one is configured
          }
      }
      .withAttributes(
        supervisionStrategy {
          case NonFatal(exception) =>
            log.error("Failed processing an export status update", exception)
            Supervision.resume
        }
      )
      .runWith(Sink.ignore)

  }
}
