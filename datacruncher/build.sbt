name := "datacruncher"
version := "0.0.1"

val sparkVersion = "3.1.2"
libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion
libraryDependencies += "org.apache.spark" %% "spark-mllib" % sparkVersion
// libraryDependencies += "org.apache.spark" %% "spark-ml" % sparkVersion
// https://mvnrepository.com/artifact/com.johnsnowlabs.nlp/spark-nlp
libraryDependencies += "com.johnsnowlabs.nlp" %% "spark-nlp" % "3.3.0"
libraryDependencies += "com.typesafe" % "config" % "1.4.1"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.2"



lazy val root = (project in file(".")).settings(
  // inThisBuild(
  //   List(
  //     organization := "fyi.newssnips",
  //     scalaVersion := "2.12.13"
  //   )
  // ),
  // sparkComponents := Seq(),
  // javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  javaOptions ++= Seq(
    "-Xms512M",
    "-Xmx2048M",
    "-XX:MaxPermSize=2048M",
    "-XX:+CMSClassUnloadingEnabled"
  )
  // scalacOptions ++= Seq("-deprecation", "-unchecked")
  // parallelExecution in Test := false,
  // fork := true,
  // coverageHighlighting := true,

  // uses compile classpath for the run task, including "provided"
  // jar (cf http://stackoverflow.com/a/21803413/3827)
  // run in Compile := Defaults
  //   .runTask(
  //     fullClasspath in Compile,
  //     mainClass in (Compile, run),
  //     runner in (Compile, run)
  //   )
  //   .evaluated,
  // scalacOptions ++= Seq("-deprecation", "-unchecked"),
  // pomIncludeRepository := { x => false },
  // resolvers ++= Seq(
  //   "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/",
  //   "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  //   "Second Typesafe repo" at "https://repo.typesafe.com/typesafe/maven-releases/",
  //   Resolver.sonatypeRepo("public")
  // ),
  // pomIncludeRepository := { _ => false }

  // publish settings
  // publishTo := {
  //   val nexus = "https://oss.sonatype.org/"
  //   if (isSnapshot.value)
  //     Some("snapshots" at nexus + "content/repositories/snapshots")
  //   else
  //     Some("releases" at nexus + "service/local/staging/deploy/maven2")
  // }
)

// https://sbt-native-packager.readthedocs.io/en/latest/formats/docker.html#requirements
// https://medium.com/jeroen-rosenberg/lightweight-docker-containers-for-scala-apps-11b99cf1a666
// https://dvirf1.github.io/play-tutorial/posts/dockerize-the-app/enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

import com.typesafe.sbt.packager.docker.DockerChmodType
import com.typesafe.sbt.packager.docker.DockerPermissionStrategy
import com.typesafe.sbt.packager.docker.DockerVersion

dockerChmodType := DockerChmodType.UserGroupWriteExecute
dockerPermissionStrategy := DockerPermissionStrategy.CopyChown

Docker / packageName := "fyi.newssnips"
Docker / version := sys.env.getOrElse("BUILD_NUMBER", "0")
Docker / daemonUserUid := None
Docker / daemonUser := "daemon"
// dockerExposedPorts := Seq(9000)
dockerBaseImage := "openjdk:11-slim"
dockerUpdateLatest := true
dockerVersion := Some(DockerVersion(20, 10, 7, Some("ce")))
