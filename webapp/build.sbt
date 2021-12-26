scalaVersion := "2.12.10"
name := """rss-to-email"""
organization := "fyi.newssnips"
version := "1.0"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test

val sparkVersion = "3.1.2"
libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion

libraryDependencies += "ch.qos.logback"              % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging"   % "3.9.4"

libraryDependencies += "com.datastax.spark" % "spark-cassandra-connector_2.12" % "3.1.0"

libraryDependencies += "com.typesafe" % "config" % "1.4.1"

// https://stackoverflow.com/questions/49760733/caused-by-java-lang-classnotfoundexception-com-sun-tools-javac-code-typetags-w
libraryDependencies += "org.projectlombok" % "lombok" % "1.18.2"

libraryDependencies += "com.redislabs" %% "spark-redis" % "3.0.0"
// libraryDependencies += "redis.clients" % "jedis" % "3.7.0"


// libraryDependencies += "org.webjars" % "swagger-ui" % "3.43.0"
lazy val root = (project in file(".")).enablePlugins(
  PlayScala
// SwaggerPlugin
).settings(
  javaOptions ++= Seq(
    "-Xms100M",
    "-Xmx512M",
    "-XX:+UseG1GC",
  )
)
// swaggerDomainNameSpaces := Seq("models")

scalafmtOnCompile := true

// https://sbt-native-packager.readthedocs.io/en/latest/formats/docker.html#requirements
// https://medium.com/jeroen-rosenberg/lightweight-docker-containers-for-scala-apps-11b99cf1a666
// https://dvirf1.github.io/play-tutorial/posts/dockerize-the-app/
import com.typesafe.sbt.packager.docker.DockerChmodType
import com.typesafe.sbt.packager.docker.DockerPermissionStrategy
dockerChmodType := DockerChmodType.UserGroupWriteExecute
dockerPermissionStrategy := DockerPermissionStrategy.CopyChown
import com.typesafe.sbt.packager.docker.DockerVersion

Docker / packageName := "rss-to-email"
Docker / version := sys.env.getOrElse("BUILD_NUMBER", "0")
Docker / daemonUserUid := None
Docker / daemonUser := "daemon"
dockerExposedPorts := Seq(9000)
dockerBaseImage := "openjdk:11-slim"
dockerUpdateLatest := true
dockerVersion := Some(DockerVersion(20, 10, 7, Some("ce")))
