scalaVersion := "2.12.10"
name := """newssnips-webapp"""
organization := "fyi.newssnips"
version := "1.0"

inThisBuild(
  List(
    scalaVersion := "2.12.10",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)

libraryDependencies += guice

val sparkVersion = "3.1.2"
libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion

libraryDependencies += "ch.qos.logback"              % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging"   % "3.9.4"

libraryDependencies += "com.datastax.spark" %% "spark-cassandra-connector" % "3.1.0"

// libraryDependencies += "com.datastax.oss" % "java-driver-query-builder" % "4.1.0"
// libraryDependencies ++= Seq(
//   "com.outworkers"  %% "phantom-dsl" % "2.59.0"
// )
// val cassandraDriver = "4.9.0"
// libraryDependencies += "com.datastax.oss"    % "java-driver-core"              % cassandraDriver
// libraryDependencies += "com.datastax.oss"    % "java-driver-query-builder"     % cassandraDriver
// libraryDependencies += "com.datastax.cassandra" % "cassandra-driver-mapping" % 3.4.0"
// libraryDependencies += "com.fasterxml.jackson.core" %% "jackson-databind" % "2.11.0"
// libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.11.4"


libraryDependencies += "com.typesafe" % "config" % "1.4.1"
// https://stackoverflow.com/questions/49760733/caused-by-java-lang-classnotfoundexception-com-sun-tools-javac-code-typetags-w
libraryDependencies += "org.projectlombok" % "lombok" % "1.18.2"

libraryDependencies += "com.redislabs" %% "spark-redis" % "3.0.0"
// libraryDependencies += "redis.clients" % "jedis" % "3.7.0"

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test

libraryDependencies ++= Seq(
  caffeine
)
libraryDependencies +=  "redis.clients" % "jedis" % "3.7.0"

// libraryDependencies += "org.webjars" % "swagger-ui" % "3.43.0"
lazy val root = (project in file(".")).enablePlugins(
  PlayScala
// SwaggerPlugin
).settings(
  // export JAVA_OPTS="-Xms100M -Xmx512M -Xss520k -XX:+UseG1GC"
  javaOptions ++= Seq(
    "-Xms100M",
    "-Xmx512M",
    "-XX:+UseG1GC",
  )
)
// swaggerDomainNameSpaces := Seq("models")

scalafmtOnCompile := true
// scalafixOnCompile := true
scalacOptions += "-Ywarn-unused"

// https://sbt-native-packager.readthedocs.io/en/latest/formats/docker.html#requirements
// https://medium.com/jeroen-rosenberg/lightweight-docker-containers-for-scala-apps-11b99cf1a666
// https://dvirf1.github.io/play-tutorial/posts/dockerize-the-app/
import com.typesafe.sbt.packager.docker.DockerChmodType
import com.typesafe.sbt.packager.docker.DockerPermissionStrategy
dockerChmodType := DockerChmodType.UserGroupWriteExecute
dockerPermissionStrategy := DockerPermissionStrategy.CopyChown
import com.typesafe.sbt.packager.docker.DockerVersion

Docker / packageName := "newssnips"
Docker / version := sys.env.getOrElse("BUILD_NUMBER", "0")
Docker / daemonUserUid := None
Docker / daemonUser := "daemon"
dockerExposedPorts := Seq(9000)
dockerBaseImage := "openjdk:11-slim"
dockerUpdateLatest := true
dockerVersion := Some(DockerVersion(20, 10, 7, Some("ce")))
