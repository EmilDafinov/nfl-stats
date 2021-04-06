package com.github.dafutils.nflstats.http.graphql

import com.github.dafutils.nflstats.db.Tables.{ExportsRow, RushingStatsRow}
import com.github.dafutils.nflstats.service.{AbstractRushingServicesModule, RushingServicesModule}
import com.github.dafutils.nflstats.{AkkaDependenciesModule, ExportRequest, Page, PagedSelection, Selection}
import sangria.schema._

import java.util.UUID

trait GraphqlSchemaModule {

  this: AkkaDependenciesModule with AbstractRushingServicesModule =>

  val defaultPageSize = 50
  val maxPageSize = 100

  val export = ObjectType(
    name = "export",
    fields = fields[Unit, ExportsRow](
      Field(
        name = "uuid",
        fieldType = UuidType,
        resolve = ctx => ctx.value.uuid
      ),
      Field(
        name = "createdOn",
        fieldType = StringType,
        resolve = ctx => ctx.value.createdOn.toInstant.toString
      ),
      Field(
        name = "status",
        fieldType = exportStatus,
        resolve = ctx => ctx.value.status
      ),
    )
  )

  val playerStats = ObjectType(
    name = "PlayerStats",
    fields = fields[Unit, RushingStatsRow](
      Field(
        name = "player",
        fieldType = StringType,
        resolve = ctx => ctx.value.player
      ),
      Field(
        name = "team",
        fieldType = StringType,
        resolve = ctx => ctx.value.team
      ),
      Field(
        name = "pos",
        fieldType = StringType,
        resolve = ctx => ctx.value.position
      ),
      Field(
        name = "att",
        fieldType = IntType,
        resolve = ctx => ctx.value.rushingAttempts
      ),
      Field(
        name = "att_G",
        fieldType = BigDecimalType,
        resolve = ctx => ctx.value.rushingAttemptsPerGame
      ),
      Field(
        name = "yds",
        fieldType = IntType,
        resolve = ctx => ctx.value.rushingYards
      ),
      Field(
        name = "avg",
        fieldType = BigDecimalType,
        resolve = ctx => ctx.value.rushingAverageYardsPerAttempt
      ),
      Field(
        name = "yds_G",
        fieldType = BigDecimalType,
        resolve = ctx => ctx.value.rushingYardsPerGame
      ),
      Field(
        name = "td",
        fieldType = IntType,
        resolve = ctx => ctx.value.totalRushingTouchdowns
      ),
      Field(
        name = "lng",
        fieldType = StringType,
        resolve = ctx =>
          if (ctx.value.touchdownOccurred)
            ctx.value.longestRush.toString + "T"
          else ctx.value.longestRush.toString
      ),
      Field(
        name = "rushingFirstDowns",
        fieldType = IntType,
        resolve = ctx => ctx.value.rushingFirstDowns
      ),
      Field(
        name = "rushingFirstDownPercentage",
        fieldType = BigDecimalType,
        resolve = ctx => ctx.value.rushingFirstDownsPercentage
      ),
      Field(
        name = "twentyPlus",
        fieldType = IntType,
        resolve = ctx => ctx.value.rushing20PlusYards
      ),
      Field(
        name = "fortyPlus",
        fieldType = IntType,
        resolve = ctx => ctx.value.rushing40PlusYards
      ),
      Field(
        name = "fum",
        fieldType = IntType,
        resolve = ctx => ctx.value.rushingFumbles
      ),
    )
  )

  val pagedSelection: Argument[PagedSelection] = Argument(
    name = "pagedSelection",
    argumentType = pagedSelectionInputType
  )

  val selection: Argument[Selection] = Argument(
    name = "selection",
    argumentType = selectionInputType
  )

  val page: Argument[Page] = Argument(
    name = "page",
    argumentType = pageInputType
  )

  val schemaRootQuery = ObjectType(
    name = "Query",
    fields = fields[UUID, Unit](
      Field(
        name = "rushingStats",
        fieldType = ListType(playerStats),
        arguments = pagedSelection :: Nil,
        resolve = ctx => {
          val selectionRequested = ctx.arg(pagedSelection)
          
          require(selectionRequested.page.size <= maxPageSize, s"Requested page size over the limit of $maxPageSize")
          
          rushingStatsRepository.list(selectionRequested)
        }
      ),
      Field(
        name = "exports",
        fieldType = ListType(export),
        arguments = page :: Nil,
        resolve = ctx => {
          val pageRequested = ctx.arg(page)

          require(pageRequested.size <= maxPageSize, s"Requested page size over the limit of $maxPageSize")
          
          exportsRepository.readExportsBelongingTo(
            ownerUuid = ctx.ctx,
            page = pageRequested
          )
        }
      ),
    )
  )

  val schemaRootMutation = ObjectType(
    name = "Mutation",
    fields = fields[UUID, Unit](
      Field(
        name = "exportRushingStats",
        fieldType = UuidType,
        arguments = selection :: Nil,
        resolve = ctx => {
          exportRequestService.requestExport(
            exportRequest = ExportRequest(
              selection = ctx.arg(selection),
              ownerUserUuid = ctx.ctx
            )
          )
        }
      )
    )
  )

  lazy val schema: Schema[UUID, Unit] = Schema(query = schemaRootQuery, mutation = Some(schemaRootMutation))
}
