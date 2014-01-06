name := "Mapper"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)

libraryDependencies ++= Seq(
  "postgresql" % "postgresql" % "9.1-901.jdbc4",
  "org.facebook4j" % "facebook4j-core" % "[1.0,)",
  "org.twitter4j" % "twitter4j-core" % "[3.0,)",
  "org.apache.commons" % "commons-email" % "1.3.2"
)

emberJsVersion := "1.2.0"

play.Project.playScalaSettings

scalacOptions ++= Seq("-feature")


