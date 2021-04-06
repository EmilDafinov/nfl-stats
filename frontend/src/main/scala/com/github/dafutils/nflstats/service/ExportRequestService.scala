package com.github.dafutils.nflstats.service

import akka.Done
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.Materializer
import akka.stream.Supervision.stop
import akka.stream.alpakka.amqp.WriteMessage
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import com.github.dafutils.nflstats.ExportRequest
import com.github.dafutils.nflstats.db.ExportsRepository
import com.github.dafutils.nflstats.json.JsonSupport._
import com.typesafe.scalalogging.Logger
import org.json4s.jackson.Serialization.write

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ExportRequestService(requestPublishingFlowWithConfirmation: Flow[WriteMessage, Boolean, Future[Done]],
                           exportsRepository: ExportsRepository) {

  private val log = Logger(this.getClass)

  def requestExport(exportRequest: ExportRequest)(implicit ec: ExecutionContext, mat: Materializer): Future[UUID] = {
    Source
      .future(
        exportsRepository.createExport(
          owner = exportRequest.ownerUserUuid,
          exportUuid = exportRequest.exportUuid,
          originalRequest = serialization.write(exportRequest)
        )
      )
      .map(_ => WriteMessage(ByteString(write(exportRequest))))
      .via(requestPublishingFlowWithConfirmation)
      .map {
        case true => exportRequest.exportUuid
        case false => throw new RuntimeException("Failed to enqueue export request")
      }
      .withAttributes(
        supervisionStrategy {
          case NonFatal(ex) =>
            log.error("Failed handling an export request", ex)
            stop
        }
      )
      .runWith(Sink.head)
  }
}
