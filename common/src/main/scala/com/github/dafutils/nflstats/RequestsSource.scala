package com.github.dafutils.nflstats

import akka.NotUsed
import akka.stream.alpakka.amqp.scaladsl.{AmqpSource, CommittableReadResult}
import akka.stream.alpakka.amqp.{AmqpConnectionProvider, NamedQueueSourceSettings}
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.Logger
import org.json4s.Formats
import org.json4s.jackson.JsonMethods._

object RequestsSource {

  def apply[T: Manifest](connectionProvider: AmqpConnectionProvider, requestQueueName: String)
                        (implicit format: Formats): Source[(CommittableReadResult, T), NotUsed] = {

    val log = Logger(this.getClass)
    AmqpSource.committableSource(
      settings = NamedQueueSourceSettings(
        connectionProvider = connectionProvider,
        queue = requestQueueName
      ),
      bufferSize = 50
    )
    .map { readResult =>
      log.info(s"Received message: ${readResult.message.bytes.utf8String}")    
       readResult -> parse(readResult.message.bytes.utf8String).extract[T]
    }
  }
}
