package com.github.dafutils.nflstats.exports

import akka.actor.ActorSystem
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.Supervision
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import akka.{Done, NotUsed}
import com.github.dafutils.nflstats.db.ExportStatus.{FAILED, SUCCESSFUL}
import com.github.dafutils.nflstats.json.JsonSupport._
import com.github.dafutils.nflstats.{ExportCompleted, ExportRequest}
import com.typesafe.scalalogging.Logger
import org.json4s.jackson.Serialization

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ExportRequestProcessor(exportRequestsSource: Source[(CommittableReadResult, ExportRequest), NotUsed],
                             exportStatusMessageSink: Sink[ByteString, Future[Done]],
                             exportGenerationService: ExportGenerationService) {

  private val log = Logger(this.getClass)
  private val maxParallelRequests = 1 //TODO: We can make this configurable

  def listenForExports()(implicit system: ActorSystem, ec: ExecutionContext): Future[Done] = {
    exportRequestsSource
      .mapAsyncUnordered(maxParallelRequests) { case (rabbitMqMessage, parsedRequest) =>
        log.info(s"Received export request $parsedRequest")
        exportGenerationService
          .`export`(parsedRequest)
          .map { _ =>
            log.info(s"Successfully processed export request ${parsedRequest.exportUuid}")
            parsedRequest.exportUuid -> SUCCESSFUL
          }
          .recover {
            case NonFatal(ex) =>
              log.error(s"Failed processing export request ${parsedRequest.exportUuid}", ex)

              parsedRequest.exportUuid -> FAILED
          }
          .andThen {
            case _ => rabbitMqMessage.ack()
          }
      }
      .flatMapConcat { case (exportUuid, exportStatus) =>
        Source
          .single(ExportCompleted(exportUuid = exportUuid, status = exportStatus))
          .map(exportCompletedMsg => ByteString(serialization.write(exportCompletedMsg)))
      }
      .withAttributes(
        supervisionStrategy {
          case NonFatal(_) => Supervision.resume
        }
      )
      .runWith(exportStatusMessageSink)
  }
}
