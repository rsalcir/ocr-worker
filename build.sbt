name := "ocr-worker"
version := "0.1"
scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka_2.10" % "0.10.0.0" withSources() exclude("org.slf4j","slf4j-log4j12") exclude("javax.jms", "jms") exclude("com.sun.jdmk", "jmxtools") exclude("com.sun.jmx", "jmxri"),
  "org.apache.spark" % "spark-core_2.10" % "1.2.1",
  "org.apache.spark" % "spark-streaming_2.10" % "1.2.1",
  "org.apache.spark" % "spark-streaming-kafka_2.10" % "1.2.1",
  "org.scalaj" %% "scalaj-http" % "2.4.0"
)

enablePlugins(DockerPlugin)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

dockerfile in docker := {
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("openjdk:8-jre")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}

