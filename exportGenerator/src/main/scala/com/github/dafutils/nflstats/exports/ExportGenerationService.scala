package com.github.dafutils.nflstats.exports

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.github.dafutils.nflstats.ExportRequest
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ExportGenerationService(rushingStatsRepository: RushingStatsExportRepository,
                              sinkGenerator: String => Sink[ByteString, Future[Done]]) {

  private val log = Logger(this.getClass)

  def export(exportRequest: ExportRequest)
            (implicit system: ActorSystem, executionContext: ExecutionContext): Future[Done] = {

    rushingStatsRepository
      .stream(exportRequest.selection)
      .map { record =>
        List(
          record.player,
          record.team,
          record.position,
          record.rushingAttempts,
          record.rushingAttemptsPerGame,
          record.rushingYards,
          record.rushingAverageYardsPerAttempt,
          record.rushingYardsPerGame,
          record.totalRushingTouchdowns,
          record.longestRush,
          record.touchdownOccurred,
          record.rushingFirstDowns,
          record.rushingFirstDownsPercentage,
          record.rushing20PlusYards,
          record.rushing40PlusYards,
          record.rushingFumbles,
        ).mkString("", ",", "\n")

      }
      .map(ByteString.apply)
      .runWith(
        sinkGenerator(
          s"exports/${exportRequest.ownerUserUuid.toString}/export_${exportRequest.exportUuid.toString}.csv"
        )
      )
      .recover {
        case NonFatal(ex) =>
          log.error(s"Failed uploading export with id  ${exportRequest.exportUuid}", ex)
          throw ex
      }
  }
}
