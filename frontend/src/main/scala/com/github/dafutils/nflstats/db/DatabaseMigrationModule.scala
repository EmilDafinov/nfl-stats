package com.github.dafutils.nflstats.db

import com.github.dafutils.nflstats.configuration.ConfigurationModule
import com.github.dafutils.nflstats.AkkaDependenciesModule
import com.typesafe.scalalogging.Logger
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.flywaydb.core.Flyway

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait DatabaseMigrationModule {

  this: ConfigurationModule with AkkaDependenciesModule =>
  val dbConfiguration = config.getConfig("nfl.db")
  val appJdbcUrl = dbConfiguration.getString("url")
  val user = dbConfiguration.getString("user")
  val password = dbConfiguration.getString("password")
  
  private val hikariConfig: HikariConfig = 
    new HikariConfig {
      setJdbcUrl(appJdbcUrl)
      setUsername(user)
      setPassword(password)
    }

  private val log = Logger(this.getClass)

  def migrateDatabase()(implicit executionContext: ExecutionContext): Future[Unit] =
    Future {
      val flywayDatasource = new HikariDataSource(hikariConfig)
      Flyway
        .configure()
        .connectRetries(30)
        .dataSource(flywayDatasource)
        .load()
        .migrate()

      flywayDatasource.close()
    } recover {
      case NonFatal(ex) =>
        log.error("Encountered error during applying / verifying database migrations.", ex)
        throw ex
    }
}
