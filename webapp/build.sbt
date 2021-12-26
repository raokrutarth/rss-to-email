scalaVersion := "2.12.10"
name := """rss-to-email"""
organization := "com.kr"
version := "1.0"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test

val sparkVersion = "3.1.2"
libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion
libraryDependencies += "org.apache.spark" %% "spark-mllib" % sparkVersion
// libraryDependencies += "org.apache.spark" %% "spark-ml" % sparkVersion
// https://mvnrepository.com/artifact/com.johnsnowlabs.nlp/spark-nlp
libraryDependencies += "com.johnsnowlabs.nlp" %% "spark-nlp" % "3.3.0"

// libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.3"

libraryDependencies += "com.typesafe" % "config" % "1.4.1"

// https://stackoverflow.com/questions/49760733/caused-by-java-lang-classnotfoundexception-com-sun-tools-javac-code-typetags-w
libraryDependencies += "org.projectlombok" % "lombok" % "1.18.2"

// libraryDependencies += "org.webjars" % "swagger-ui" % "3.43.0"
lazy val root = (project in file(".")).enablePlugins(
  PlayScala
// SwaggerPlugin
)
// swaggerDomainNameSpaces := Seq("models")

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

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.kr.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.kr.binders._"
