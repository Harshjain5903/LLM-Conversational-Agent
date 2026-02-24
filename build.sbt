ThisBuild / version := "1.0.0"

ThisBuild / scalaVersion := "3.5.0"

lazy val root = (project in file("."))
  .settings(
    name := "LLMConversationalAgent",
    organization := "com.hardas",
    
    // JVM Settings
    javacOptions ++= Seq(
      "-source", "11",
      "-target", "11",
      "-encoding", "UTF-8"
    ),
    
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-encoding", "UTF-8"
    )
  )

// Scala and JVM Versions
val akkaVersion = "2.9.3"
val akkaHttpVersion = "10.6.3"
val grpcVersion = "1.63.0"
val scalapbVersion = "0.11.15"

// Core Dependencies
libraryDependencies ++= Seq(
  // Akka
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  
  // gRPC
  "io.grpc" % "grpc-netty-shaded" % grpcVersion,
  "io.grpc" % "grpc-protobuf" % grpcVersion,
  "io.grpc" % "grpc-stub" % grpcVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion % "protobuf",
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapbVersion,
  
  // Configuration Management
  "com.typesafe" % "config" % "1.4.3",
  
  // Logging
  "org.slf4j" % "slf4j-api" % "2.0.11",
  "ch.qos.logback" % "logback-classic" % "1.5.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  
  // AWS SDK for Java
  "software.amazon.awssdk" % "bedrock" % "2.26.1",
  "software.amazon.awssdk" % "bedrock-runtime" % "2.26.1",
  "software.amazon.awssdk" % "lambda" % "2.26.1",
  "software.amazon.awssdk" % "apigateway" % "2.26.1",
  
  // JSON Processing
  "com.google.code.gson" % "gson" % "2.11.0",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.17.0",
  
  // HTTP Client
  "com.softwaremill.sttp.client3" %% "core" % "3.9.1",
  "com.softwaremill.sttp.client3" %% "akka-http-backend" % "3.9.1",
  
  // Utilities
  "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.2",
  
  // Testing
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.17" % Test,
  "org.scalatestplus" %% "scalacheck-1-17" % "3.2.17.0" % Test,
  "org.mockito" % "mockito-core" % "5.7.1" % Test
)

// Protobuf Compile Settings
Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

// Assembly Plugin for creating fat JAR
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.5")

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "services", "org.apache.hadoop.fs.FileSystem") =>
    MergeStrategy.concat
  case PathList("META-INF", xs @ _*) =>
    (xs map {_.toLowerCase}) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "services" :: _ => MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.first
    }
  case _ => MergeStrategy.first
}