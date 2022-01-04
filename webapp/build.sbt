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
libraryDependencies ++= Seq(
"org.postgresql" % "postgresql" % "42.3.1",
"com.zaxxer" % "HikariCP" % "5.0.0",
"com.typesafe" % "config" % "1.4.1",
// https://stackoverflow.com/questions/49760733/caused-by-java-lang-classnotfoundexception-com-sun-tools-javac-code-typetags-w
"org.projectlombok" % "lombok" % "1.18.2",
"org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
"redis.clients" % "jedis" % "3.7.0"
)
// https://mvnrepository.com/artifact/com.sendgrid/sendgrid-java
libraryDependencies += "com.sendgrid" % "sendgrid-java" % "4.8.0" excludeAll(
    ExclusionRule(organization = "com.fasterxml.jackson.core")
)

// avoid: SLF4J: Class path contains multiple SLF4J bindings
libraryDependencies := libraryDependencies.value.map(_.exclude("org.slf4j", "*"))
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.7"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"

libraryDependencies ++= Seq(
  caffeine
)

// libraryDependencies += "org.webjars" % "swagger-ui" % "3.43.0"
lazy val root = (project in file(".")).enablePlugins(
  PlayScala
// SwaggerPlugin
)
// .settings(
//   // export JAVA_OPTS="-Xms100M -Xmx512M -Xss520k -XX:+UseG1GC"
//   javaOptions ++= Seq(
//     "-Xms100M",
//     "-Xmx512M",
//     "-XX:+UseG1GC",
//   )
// )
// swaggerDomainNameSpaces := Seq("models")

scalafmtOnCompile := true
scalafixOnCompile := true
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
dockerBaseImage := "openjdk:8-jre-alpine"
dockerUpdateLatest := true
dockerVersion := Some(DockerVersion(20, 10, 7, Some("ce")))
