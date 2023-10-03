import Dependencies._

lazy val root =
  project
    .in(file("."))
    .settings(
      name := "akka-bundle"
    )

lazy val `akka-essentials-typed` =
  project
    .in(file("akka-essentials-typed"))
    .settings(
      name := "akka-essentials-typed",
      libraryDependencies ++= Seq(akka, akkaTestkit, scalaTest, logback)
    )
