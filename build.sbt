import Dependencies._

lazy val root =
  project
    .in(file("."))
    .settings(
      name := "akka-bundle"
    )
    .aggregate(`akka-essentials-typed`, `akka-persistence`)

lazy val `akka-essentials-typed` =
  project
    .in(file("akka-essentials-typed"))
    .settings(
      name := "akka-essentials-typed",
      libraryDependencies ++= Seq(akka, akkaTestkit, scalaTest, logback)
    )

lazy val `akka-persistence` =
  project
    .in(file("akka-persistence"))
    .settings(
      name := "akka-persistence",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-persistence" % "2.5.13",

        // local levelDB stores
        "org.iq80.leveldb" % "leveldb" % "0.7",
        "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",

        // JDBC with PostgreSQL
        "org.postgresql" % "postgresql" % "42.2.2",
        "com.github.dnvriend" %% "akka-persistence-jdbc" % "3.4.0",

        // Cassandra
        "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.91",
        "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % "0.91" % Test,

        // Google Protocol Buffers
        "com.google.protobuf" % "protobuf-java" % "3.6.1"
      )
    )
