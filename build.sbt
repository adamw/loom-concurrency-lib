import com.softwaremill.SbtSoftwareMillCommon.commonSmlBuildSettings

lazy val commonSettings = commonSmlBuildSettings ++ Seq(
  organization := "com.softwaremill.loom",
  scalaVersion := "3.3.1"
)

lazy val rootProject = (project in file("."))
  .settings(commonSettings)
  .settings(publishArtifact := false, name := "loom-concurrency-lib")
  .aggregate(core)

lazy val core: Project = (project in file("core"))
  .settings(commonSettings)
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "com.softwaremill.ox" %% "core" % "0.0.15",
      "ch.qos.logback" % "logback-classic" % "1.4.11"
    )
  )
