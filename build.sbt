scalaVersion := "2.11.8"

version := "0.1"

libraryDependencies ++= Seq("s3", "kms").map(module => 
  "com.amazonaws" % s"aws-java-sdk-$module" % "1.11.30"
)
libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.5.0",
  "commons-io" % "commons-io" % "2.5"
)
