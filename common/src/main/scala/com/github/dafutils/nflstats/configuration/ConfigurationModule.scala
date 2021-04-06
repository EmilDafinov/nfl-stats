package com.github.dafutils.nflstats.configuration

import com.typesafe.config.ConfigFactory

trait ConfigurationModule {

  lazy val config = ConfigFactory.load()
}
