package com.github.dafutils.nflstats.rabbitmq

import akka.stream.alpakka.amqp.{AmqpCachedConnectionProvider, AmqpConnectionProvider, AmqpCredentials, AmqpDetailsConnectionProvider}
import com.github.dafutils.nflstats.configuration.ConfigurationModule

trait RabbitMqModule {
  this: ConfigurationModule => 
  
  lazy val rabbitmqConfig = config.getConfig("rabbitmq")
  
  lazy val connectionProvider: AmqpConnectionProvider = AmqpCachedConnectionProvider(
    AmqpDetailsConnectionProvider(
      host = rabbitmqConfig.getString("host"), 
      port = rabbitmqConfig.getInt("port")
    )
    .withCredentials(
      amqpCredentials = AmqpCredentials(
        username = rabbitmqConfig.getString("user"),
        password = rabbitmqConfig.getString("password")
      )
    )
    .withVirtualHost(virtualHost = rabbitmqConfig.getString("vhost"))
  )
}
