package com.github.dafutils.nflstats.http.graphql

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive, MalformedRequestContentRejection}
import com.github.dafutils.nflstats.json.JsonSupport._
import org.json4s.JsonAST
import org.json4s.JsonAST.{JObject, JString}
import sangria.ast.Document
import sangria.parser.QueryParser

import scala.util.{Failure, Success, Try}

object GraphQlParser {

  private def parseGraphQl(requestJson: JObject): Try[(Document, Option[String], JsonAST.JValue)] = {

    for {
      maybeQuery <- Try(
        requestJson.findField {
          case ("query", JString(_)) => true
          case _ => false
        } map {
          case (_, JString(fieldValue)) => fieldValue
        } getOrElse {
          throw new IllegalArgumentException("The request does not include a query field")
        }
      )

      maybeOperation = requestJson.findField {
        case ("operationName", JString(_)) => true
        case _ => false
      } map {
        case (_, JString(fieldValue)) => fieldValue
      }

      vars = requestJson.findField {
        case ("variables", JObject(_)) => true
        case _ => false
      } map {
        case (_, variablesObject) => variablesObject
      } getOrElse JObject(obj = List.empty)

      parsedQuery <- QueryParser.parse(maybeQuery)
    } yield (parsedQuery, maybeOperation, vars)

  }

  def apply(): Directive[(Document, Option[String], JsonAST.JValue)] = {
    entity(as[JObject])
      .map(parseGraphQl)
      .flatMap {
        case Success((parsedQuery, maybeOperation, variables)) => tprovide((parsedQuery, maybeOperation, variables))
        case Failure(error) =>
          reject(MalformedRequestContentRejection(message = "GraphQL query parsing failed", cause = error))
      }
  }
}
