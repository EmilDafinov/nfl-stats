package com.github.dafutils.nflstats.http

import com.github.dafutils.nflstats.{Page, PagedSelection, Selection, SortBy, SortField, SortOrder}
import com.github.dafutils.nflstats.db.ExportStatus._
import com.github.dafutils.nflstats.SortField.{LNG, TD, YDS}
import com.github.dafutils.nflstats.json.JsonSupport._
import org.json4s.JValue
import sangria.ast.StringValue
import sangria.macros.derive._
import sangria.marshalling.json4s.jackson.Json4sJacksonResultMarshaller
import sangria.marshalling.{FromInput, ResultMarshaller}
import sangria.schema._
import sangria.validation.ValueCoercionViolation

import java.util.UUID
import scala.util.Try

package object graphql {

  def deriveFromInput[T: Manifest]: FromInput[T] = new FromInput[T] {
    override val marshaller: ResultMarshaller = Json4sJacksonResultMarshaller

    override def fromResult(node: marshaller.Node): T = node.asInstanceOf[JValue].extract[T]
  }

  case object SortFieldCoercionViolation extends ValueCoercionViolation("SortField Value expected.")

  case object SortOrderCoercionViolation extends ValueCoercionViolation("SortOrder Value expected.")

  case object UuidCoercionViolation extends ValueCoercionViolation("UUID Value expected.")
  
  implicit val sortOrderFromInput = deriveEnumType[SortOrder.Value]()

  implicit val sortFieldFromInput = deriveEnumType[SortField.Value]()

  implicit val sportByFromInput = deriveFromInput[SortBy]

  implicit val pageFromInput = deriveFromInput[Page]

  implicit val selectionFromInput = deriveFromInput[Selection]

  implicit val pagedSelectionFromInput = deriveFromInput[PagedSelection]

  val sortByInputType = InputObjectType[SortBy](
    "SortBy",
    List(
      InputField(name = "sortField", fieldType = sortFieldFromInput),
      InputField(name = "sortOrder", fieldType = sortOrderFromInput),
    )
  )

  val selectionInputType = InputObjectType[Selection](
    name = "Selection",
    fields = List(
      InputField(name = "requestedPlayers", fieldType = ListInputType(StringType)),
      InputField(name = "maybeSortBy", fieldType = OptionInputType(sortByInputType)),
    )
  )

  val pageInputType = InputObjectType[Page](
    name = "Page",
    fields = List(
      InputField(name = "index", fieldType = IntType),
      InputField(name = "size", fieldType = IntType),
    )
  )

  val pagedSelectionInputType = InputObjectType[PagedSelection](
    name = "PagedSelection",
    fields = List(
      InputField(name = "selection", fieldType = selectionInputType),
      InputField(name = "page", fieldType = pageInputType),
    )
  )

  val sortFieldEnum = EnumType(
    name = "SortField",
    values = List(
      EnumValue(name = "Yds", value = YDS),
      EnumValue(name = "Lng", value = LNG),
      EnumValue(name = "TD", value = TD),
    )
  )

  val exportStatus = EnumType(
    name = "ExportStatus",
    values = List(
      EnumValue(name = "SUCCESSFUL", value = SUCCESSFUL),
      EnumValue(name = "IN_PROGRESS", value = IN_PROGRESS),
      EnumValue(name = "FAILED", value = FAILED),
    )
  )
  
  val UuidType = ScalarType[UUID](
    name = "UUID",
    coerceOutput = (uuid, _) => uuid.toString,
    coerceUserInput = {
      case s: String => Try(UUID.fromString(s)).toEither.left.map(_ => UuidCoercionViolation)
      case _         => Left(UuidCoercionViolation)
    },
    coerceInput = {
      case StringValue(s, _, _, _, _) => Try(UUID.fromString(s)).toEither.left.map(_ => UuidCoercionViolation)
      case _                          => Left(UuidCoercionViolation)
    }
  )
}
