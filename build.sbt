name := "scala-akka-stream-tests"

version := "1.0"

scalaVersion := "2.11.8"

val akkaVersion = "2.4.6"

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Apache repo" at "https://repository.apache.org/content/repositories/releases"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "mysql" % "mysql-connector-java" % "5.1.37",
  "org.scalaj" %% "scalaj-http" % "1.1.4"
)