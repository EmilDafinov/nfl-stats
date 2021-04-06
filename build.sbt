
val AkkaVersion = "2.6.8"
val AkkaHttpVersion = "10.2.3"
val Json4sVersion = "3.6.10"
val SlickVersion = "3.3.3"
val ScalaTestVersion = "3.2.5"

val versionSettings = Seq(
  //  The 'version' setting is not set on purpose: its value is generated automatically by the sbt-dynver plugin
  //  based on the git tag/sha. Here we're just tacking on the maven-compatible snapshot suffix if needed
  dynverSonatypeSnapshots in ThisBuild := true,
  version in ThisBuild ~= (_.replace('+', '_')),
  dynver in ThisBuild ~= (_.replace('+', '_'))
)

lazy val root = (project in file("."))
  .enablePlugins(
    JavaServerAppPackaging,
    AshScriptPlugin,
    DockerPlugin,
  )
  .settings(versionSettings)
  .settings(
    scalaVersion := "2.12.13",

    organization := "com.github.dafutils",

    name := "nfl-stats-root",
    
    dockerBaseImage := "openjdk:11.0.10-jdk-slim",

    Test / parallelExecution := false,
    publishArtifact in ThisBuild in packageDoc := false,
    publishArtifact in ThisBuild in packageSrc := false
  )
  .aggregate(common, frontend, exportGenerator)

lazy val common = project
  .settings(versionSettings)
  .settings(
    name := "nfl-stats-common",
    
    libraryDependencies ++= Seq(
      //Application config
      "com.typesafe" % "config" % "1.4.1",

      //Logging
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",

      //HTTP
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
      
      //RabbitMQ
      "com.lightbend.akka" %% "akka-stream-alpakka-amqp" % "2.0.2",

      // JSON
      "de.heikoseeberger" %% "akka-http-json4s" % "1.35.3",
      "org.json4s" %% "json4s-jackson" % Json4sVersion,
      "org.json4s" %% "json4s-ext" % Json4sVersion,
      
      //Database
      "com.typesafe.slick" %% "slick" % SlickVersion,
      "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion,
      "mysql" % "mysql-connector-java" % "5.1.46",

      //Resources
      "com.jsuereth" %% "scala-arm" % "2.0",
      
      //Test
      "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
      "com.typesafe.slick" %% "slick-codegen" % SlickVersion % Test,
      "org.scalactic" %% "scalactic" % ScalaTestVersion % Test,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
      "org.scalatestplus" %% "mockito-3-4" % "3.2.2.0" % Test,
      "org.mockito" % "mockito-core" % "3.8.0" % Test
    )
  )

lazy val frontend = project
  .dependsOn(common)
  .settings(versionSettings)
  .enablePlugins(JavaServerAppPackaging, AshScriptPlugin, DockerPlugin)
  .settings(

    name := "nfl-stats-frontend",

    libraryDependencies ++= Seq(
      
      //S3
      "com.amazonaws" % "aws-java-sdk-s3" % "1.11.974",
      
      //GraphQL
      "org.sangria-graphql" %% "sangria" % "2.1.0",
      "org.sangria-graphql" %% "sangria-json4s-jackson" % "1.0.1",
      
      //Database
      "org.flywaydb" % "flyway-core" % "5.2.4",
      
      
      
      //Test
      "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
      "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion % Test,
      "org.scalactic" %% "scalactic" % ScalaTestVersion % Test,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
      "org.scalatestplus" %% "mockito-3-4" % "3.2.2.0" % Test,
      "org.mockito" % "mockito-core" % "3.8.0" % Test
    )
  )

lazy val exportGenerator = project
  .dependsOn(common)
  .settings(versionSettings)
  .enablePlugins(JavaServerAppPackaging, AshScriptPlugin, DockerPlugin)
  .settings(

    name := "nfl-stats-export-generator",

    libraryDependencies ++= Seq(
      "com.jsuereth" %% "scala-arm" % "2.0",

      //S3
      "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "2.0.2",
      "com.typesafe.akka" %% "akka-http-xml" % AkkaHttpVersion, // possibly not needed

      //Test
      "org.scalactic" %% "scalactic" % ScalaTestVersion % Test,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
      "org.scalatestplus" %% "mockito-3-4" % "3.2.2.0" % Test,
      "org.mockito" % "mockito-core" % "3.8.0" % Test
    )
  )
