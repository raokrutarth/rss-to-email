name    := "datacruncher"
version := "0.0.1"
inThisBuild(
  List(
    scalaVersion := "2.12.13",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)

val sparkVersion = "3.1.2"
libraryDependencies += "org.apache.spark" %% "spark-sql"   % sparkVersion
libraryDependencies += "org.apache.spark" %% "spark-mllib" % sparkVersion
// libraryDependencies += "org.apache.spark" %% "spark-ml" % sparkVersion
// https://mvnrepository.com/artifact/com.johnsnowlabs.nlp/spark-nlp
libraryDependencies += "com.johnsnowlabs.nlp" %% "spark-nlp" % "3.3.3"
libraryDependencies += "com.typesafe"          % "config"    % "1.4.1"

libraryDependencies += "ch.qos.logback"              % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging"   % "3.9.4"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.2"

libraryDependencies += "com.datastax.spark" % "spark-cassandra-connector_2.12" % "3.1.0"
libraryDependencies += "org.postgresql" % "postgresql" % "42.3.1"


libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % "test"
libraryDependencies +=  "redis.clients" % "jedis" % "3.7.0"

scalacOptions += "-Ywarn-unused"
scalafmtOnCompile := true
scalafixOnCompile := true

// export JAVA_OPTS="-XX:+AggressiveHeap -Xms512M -Xmx6048M"
// export JAVA_OPTS="-Xms1024M -Xmx7048M -XX:+UseG1GC -XX:+UseStringDeduplication"
lazy val root = (project in file(".")).settings(
  javaOptions ++= Seq(
    "-Xms512M",
    "-Xmx6048M",
    "-XX:+AggressiveHeap"
  )
)
// fork := true

// https://sbt-native-packager.readthedocs.io/en/latest/formats/docker.html#requirements
// https://medium.com/jeroen-rosenberg/lightweight-docker-containers-for-scala-apps-11b99cf1a666
// https://dvirf1.github.io/play-tutorial/posts/dockerize-the-app/enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(JavaAppPackaging)

import com.typesafe.sbt.packager.docker.DockerChmodType
import com.typesafe.sbt.packager.docker.DockerPermissionStrategy
import com.typesafe.sbt.packager.docker.DockerVersion

dockerChmodType          := DockerChmodType.UserGroupWriteExecute
dockerPermissionStrategy := DockerPermissionStrategy.CopyChown

Docker / packageName   := "fyi.newssnips"
Docker / version       := sys.env.getOrElse("BUILD_NUMBER", "0")
Docker / daemonUserUid := None
Docker / daemonUser    := "daemon"

dockerBaseImage    := "openjdk:11-slim"
dockerUpdateLatest := true
dockerVersion      := Some(DockerVersion(20, 10, 7, Some("ce")))
