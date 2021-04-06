package com.github.dafutils.nflstats

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext

trait AkkaDependenciesModule {

  implicit val actorSystem = ActorSystem("my-actor-system")
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher
}
