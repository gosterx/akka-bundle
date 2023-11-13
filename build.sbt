import Dependencies._

lazy val root =
  project
    .in(file("."))
    .settings(
      name := "akka-bundle"
    )
    .aggregate(`akka-essentials-typed`, `akka-persistence`, `akka-streams`, `akka-remoting-clustering`)

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
        "org.iq80.leveldb"          % "leveldb"        % "0.7",
        "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",

        // JDBC with PostgreSQL
        "org.postgresql"       % "postgresql"            % "42.2.2",
        "com.github.dnvriend" %% "akka-persistence-jdbc" % "3.4.0",

        // Cassandra
        "com.typesafe.akka" %% "akka-persistence-cassandra"          % "0.91",
        "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % "0.91" % Test,

        // Google Protocol Buffers
        "com.google.protobuf" % "protobuf-java" % "3.6.1"
      )
    )

lazy val `akka-streams` =
  project
    .in(file("akka-streams"))
    .settings(
      name := "akka-streams",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-stream"         % "2.5.19",
        "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.19",
        "com.typesafe.akka" %% "akka-testkit"        % "2.5.19",
        "org.scalatest"     %% "scalatest"           % "3.2.9"
      )
    )

lazy val `akka-remoting-clustering` =
  project
    .in(file("akka-remoting-clustering"))
    .settings(
      name := "akka-remoting-clustering",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor"            % "2.5.21",
        "com.typesafe.akka" %% "akka-remote"           % "2.5.21",
        "com.typesafe.akka" %% "akka-cluster"          % "2.5.21",
        "com.typesafe.akka" %% "akka-cluster-sharding" % "2.5.21",
        "com.typesafe.akka" %% "akka-cluster-tools"    % "2.5.21"
      )
    )
