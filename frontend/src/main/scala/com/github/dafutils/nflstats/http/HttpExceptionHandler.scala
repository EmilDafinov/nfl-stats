package com.github.dafutils.nflstats.http

import akka.http.scaladsl.server._
import com.typesafe.scalalogging.Logger

import scala.util.control.NonFatal

object HttpExceptionHandler {

  def apply(): ExceptionHandler = {
    val log = Logger(this.getClass)
    ExceptionHandler {
      case NonFatal(ex) =>
        log.error("An exception has occurred", ex)
        throw ex
    }
  }
}
