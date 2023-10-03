import sbt._

object Dependencies {
  val akka        = "com.typesafe.akka" %% "akka-actor-typed"         % "2.6.18"
  val akkaTestkit = "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.6.18"
  val scalaTest   = "org.scalatest"     %% "scalatest"                % "3.2.9"
  val logback     = "ch.qos.logback"     % "logback-classic"          % "1.2.10"
}
