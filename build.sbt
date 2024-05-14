val info = new {
  val organization = "com.roshan"
  val name = "s3-to-kafka-zio"
  val scalaVersion = "2.13.10"
}

lazy val versions = new {
  val logback = "1.2.3"
  val logbackEcsEncoder = "1.0.1"
  val scalaLogging = "3.9.2"
  val testcontainersScalaVersion = "0.39.3"
  val scalactic = "3.2.3"
  val scalatest = "3.2.3"
  val mockServer = "5.11.2"
  val awsS3 = "1.12.192"
  val scalaCsv = "1.3.6"
  val scalaCsvParser = "0.13.1"
  val apacheKafka = "3.4.0"
  val zioKafka = "2.7.4"
  val secretManager = "1.12.261"
  val zio = "2.0.22"
  val zioJson = "0.2.0-M2"
  val zioSchema = "1.1.0"//"0.4.2"
  val zioHttp = "3.0.0-RC2"
  val zioS3 = "0.4.2.4"
  val config = "3.0.7"
  val zioFtp = "0.4.1"
  val lambdaCore = "1.2.1"
  val lambdaEvents = "3.2.0"
  val awsVersion = "2.16.61"

}

name := info.name
ThisBuild / organization := info.organization
version in ThisBuild := {
  s"$branchName-${dynverInstance.value.version(dynverCurrentDate.value)}".replace('+', '-')
}
ThisBuild / scalaVersion := info.scalaVersion
ThisBuild / scalacOptions ++= Seq(
  "-Xlint:unused",
  "-Wconf:any:warning-verbose",
  "-Wconf:src=src_managed/.*:info-summary",
  "-Wconf:src=src_managed/.*&cat=unused:silent",
  "-Ylog-classpath"
)

val branchName = sys.env.getOrElse(
  "TRAVIS_BRANCH",
  sys.process.Process("git rev-parse --symbolic-full-name --abbrev-ref HEAD").!!.dropRight(1)
)

val deploymentEnvName = sys.env.getOrElse(
  "DEPLOYMENT_ENV",
  branchName
)

lazy val root = (project in file("."))
  .settings(
    name := "s3-to-kafka-zio",
    assemblyJarName in assembly := "s3-to-kafka-zio.jar",
    assemblyOutputPath in assembly := file("sam/mw-wmos-to-pe.jar"),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs@_*) => MergeStrategy.discard
      case x => MergeStrategy.first
    },
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % versions.zio,
      "dev.zio" %% "zio-streams" % versions.zio,
      "dev.zio" %% "zio-kafka" % versions.zioKafka,
      "dev.zio" %% "zio-json" % versions.zioJson,
      "dev.zio" %% "zio-schema" % versions.zioSchema,
      "dev.zio" %% "zio-schema-json" % versions.zioSchema,
      "dev.zio" %% "zio-schema-avro" % versions.zioSchema,
      "dev.zio" %% "zio-http" % versions.zioHttp,
      "dev.zio" %% "zio-config" % versions.config,
      "dev.zio" %% "zio-s3" %  versions.zioS3,
      //"dev.zio" %% "zio-connect-s3" % "0.4.4",
      "com.amazonaws" % "aws-java-sdk-s3" % versions.awsS3,
      "org.apache.kafka" % "kafka-clients" % versions.apacheKafka,
      "com.amazonaws" % "aws-java-sdk-secretsmanager" % versions.awsS3,
      "com.amazonaws" % "aws-lambda-java-core" % versions.lambdaCore,
      "com.amazonaws" % "aws-lambda-java-events" % versions.lambdaEvents,
      "software.amazon.awssdk" % "netty-nio-client" % "2.23.10",
      "software.amazon.awssdk" % "s3" % "2.23.10",
      "dev.zio" %% "zio-ftp" % versions.zioFtp,
      "dev.zio" %% "zio-test" % versions.zio % Test,
      "org.scalatest" %% "scalatest" % versions.scalatest % Test,
      "io.confluent" % "kafka-avro-serializer" % "4.1.1",
      // https://mvnrepository.com/artifact/dev.zio/zio-aws-netty
      "dev.zio" %% "zio-aws-netty" % "7.21.15.5"

      // "dev.zio" %% "zio-aws-s3" % "7.21.15.12"

    ),
    autoCompilerPlugins := true,
    resolvers ++= Seq("aws-sdk-java".at("https://repo.maven.apache.org/maven2/"),"io.confluent" at "https://packages.confluent.io/maven/"),
    Compile / avroSpecificScalaSource := sourceManaged.value / "scala_avro",
    Compile / managedSourceDirectories += (Compile / avroSpecificScalaSource).value,
    Compile / sourceGenerators += (Compile / avroScalaGenerateSpecific).taskValue,
  )