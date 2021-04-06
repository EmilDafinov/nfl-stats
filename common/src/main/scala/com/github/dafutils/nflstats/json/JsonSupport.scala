package com.github.dafutils.nflstats.json

import com.github.dafutils.nflstats.{SortBy, SortField, SortOrder}
import com.github.dafutils.nflstats.db.ExportStatus
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.ext.{EnumNameSerializer, EnumSerializer}
import org.json4s.jackson.Serialization
import org.json4s.{DefaultFormats, Formats, Serialization}

object JsonSupport extends Json4sSupport{

  implicit lazy val serialization: Serialization = Serialization
  
  implicit lazy val formats: Formats = new DefaultFormats {
    override val allowNull = false
  } + UuidCustomSerializer + 
    new EnumSerializer(ExportStatus) + 
    new EnumNameSerializer(ExportStatus) +
    new EnumSerializer(SortField) +
    new EnumNameSerializer(SortField) +
    new EnumSerializer(SortOrder) +
    new EnumNameSerializer(SortOrder)
}
