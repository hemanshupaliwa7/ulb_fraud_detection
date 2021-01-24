import AssemblyKeys._

lazy val buildSettings = Seq(
  name := "ulb_fraud_prediction",
  version := "1.0",
  organization := "com.cldcvr",
  scalaVersion := "2.12.10"
)

val app = (project in file(".")).
  settings(buildSettings: _*).
  settings(assemblySettings: _*).
  settings(
    mergeStrategy in assembly := {
      //case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case PathList("META-INF", xs @ _*) =>
        xs map {_.toLowerCase} match {
          case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
            MergeStrategy.discard
          case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
            MergeStrategy.discard
          case "services" :: _ =>  MergeStrategy.filterDistinctLines
          case _ => MergeStrategy.first
        }
      case x => MergeStrategy.first
    }
  )

val sparkVersion = "3.0.1"
val sparklingWaterVersion = "3.30.1.3-1-3.0"

libraryDependencies ++= Seq(
  //Spark Libreries
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion,
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion,
  "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-mllib" % sparkVersion,
  "org.apache.spark" %% "spark-hive" % sparkVersion,
  "org.apache.spark" %% "spark-repl" % sparkVersion,

  "ai.h2o" %% "sparkling-water-package" % sparklingWaterVersion,

  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
)


resolvers ++= Seq(
  //"JBoss Repository" at "http://repository.jboss.org/nexus/content/repositories/releases/",
  "Maven Central" at "https://repo.maven.apache.org/maven2",
  //"Spray Repository" at "http://repo.spray.io/",
  "Cloudera Repository" at "https://repository.cloudera.com/artifactory/cloudera-repos/",
  "Akka Repository" at "http://repo.akka.io/releases/",
  "Twitter4J Repository" at "http://twitter4j.org/maven2/",
  "Apache HBase" at "https://repository.apache.org/content/repositories/releases",
  "Twitter Maven Repo" at "http://maven.twttr.com/",
  "scala-tools" at "https://oss.sonatype.org/content/groups/scala-tools",
  //"Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  //"Second Typesafe repo" at "http://repo.typesafe.com/typesafe/maven-releases/",
  "Mesosphere Public Repository" at "http://downloads.mesosphere.io/maven",
  "confluent" at "http://packages.confluent.io/maven/",
  Resolver.bintrayRepo("ovotech", "maven")
)


