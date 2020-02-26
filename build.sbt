name := "scala-bot"

version := "0.1"

scalaVersion := "2.12.4"
scalacOptions += "-Ypartial-unification"

libraryDependencies ++= {
  val akkaVersion = "2.4.12"
  Seq(
    // https://mvnrepository.com/artifact/org.scala-lang.modules/scala-xml
    "org.scala-lang.modules" %% "scala-xml" % "2.0.0-M1",
    "org.scalactic"          %% "scalactic"      % "3.0.4",
    "org.scalatest"          %% "scalatest"      % "3.0.4" % "test",
    "org.scalamock"          %% "scalamock"      % "4.0.0" % Test,
    "com.typesafe.akka"      %% "akka-actor"     % akkaVersion,
    "org.typelevel"          %% "cats-core"      % "1.5.0",
    "org.typelevel"          %% "cats-effect"    % "1.3.0"
  )
}