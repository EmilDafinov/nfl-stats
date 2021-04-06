package com.github.dafutils.nflstats.json

import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString

import java.util.UUID

object UuidCustomSerializer
  extends CustomSerializer[UUID](_ =>
    ({
      case JString(uuidToDeserialize) =>
        UUID.fromString(uuidToDeserialize)
    }, {
      case uuid: UUID =>
        JString(uuid.toString)
    }))
