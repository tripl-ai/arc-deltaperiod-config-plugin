import sbt._

object Dependencies {
  // versions
  lazy val sparkVersion = "3.3.2"

  // testing
  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.7" % "test,it"

  // arc
  val arc = "ai.tripl" %% "arc" % "3.13.2" % "provided"

  val sparkSql = "org.apache.spark" %% "spark-sql" % sparkVersion % "provided"

  // Project
  val etlDeps = Seq(
    scalaTest,

    arc,

    sparkSql
  )
}