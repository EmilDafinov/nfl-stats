package com.github.dafutils.nflstats.http

import akka.http.scaladsl.model.ContentTypes.`application/octet-stream`
import akka.http.scaladsl.model.StatusCodes.{BadRequest, Found, NotFound, OK}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.ByteString
import com.github.dafutils.nflstats.AkkaDependenciesModule
import com.github.dafutils.nflstats.http.graphql.{GraphQlParser, GraphqlSchemaModule}
import com.github.dafutils.nflstats.json.JsonSupport._
import com.github.dafutils.nflstats.service.AbstractRushingServicesModule
import com.typesafe.scalalogging.Logger
import sangria.execution.deferred.DeferredResolver
import sangria.execution.{ExceptionHandler, Executor, HandledException, QueryAnalysisError}
import sangria.marshalling.json4s.jackson._

import java.util.UUID
import scala.util.control.NonFatal


trait FrontendRouteModule extends ApplicationHttpRoute {
  this: AkkaDependenciesModule with GraphqlSchemaModule with AbstractRushingServicesModule =>

  private val log = Logger(this.getClass)

  // Very simple directive for extracting authentication information from the incoming request
  // Normally this would be more involved, but for the sake of simplicity, we assume that
  // the user uuid passed in the `user_uuid` header is trustworthy and represents the uuid of
  // the user making the request
  private val extractOwnerUuid = headerValueByName("user_uuid").map(UUID.fromString)

  private lazy val graphqlEndpointExceptionHandler = ExceptionHandler {
    case (_, NonFatal(e)) =>
      log.error(e.getMessage, e)
      HandledException(e.getMessage)
  }

  override val applicationRoute: Route = {
    (pathPrefix("health") & get) {
      complete(HttpResponse())
    } ~
      (pathPrefix("nfl")
        & handleExceptions(HttpExceptionHandler())) {

        (get & pathEndOrSingleSlash) {
          getFromResource("graphiql.html")
        } ~
          (path("graphql")
            & post
            & GraphQlParser()
            & extractOwnerUuid
            ) {
            (graphqlDocument, maybeGraphQlOperation, graphQlVariables, ownerUserUuid) =>

              complete(
                Executor
                  .execute(
                    schema = schema,
                    queryAst = graphqlDocument,
                    userContext = ownerUserUuid,
                    variables = graphQlVariables,
                    operationName = maybeGraphQlOperation,
                    deferredResolver = DeferredResolver.empty,
                    exceptionHandler = graphqlEndpointExceptionHandler,
                  )
                  .map(OK -> _)
                  .recover {
                    case error: QueryAnalysisError =>
                      log.error("Error encountered during graphql query analysis", error)
                      BadRequest -> error.resolveError
                  }
              )
          } ~
          (path("exports" / JavaUUID)
            & get
            & extractOwnerUuid
            & rejectEmptyResponse) { (exportUuid, userUuid) =>
            
            log.info(s"Downloading export with ID ${exportUuid.toString}")

            complete(
              exportDownloadService
                .downloadExport(exportUuid = exportUuid, ownerUuid = userUuid)
                .map {
                  _.map { presignedURL =>
                    val redirectUri = Uri(presignedURL.toString)
                    HttpResponse(
                      status = Found,
                      headers = headers.Location(redirectUri) :: Nil,
                      entity = HttpEntity.Strict(
                        contentType = `application/octet-stream`,
                        data = ByteString(Found.htmlTemplate.format(redirectUri))
                      )
                    )
                  }
                }
            )
          }
      }
  }
}
