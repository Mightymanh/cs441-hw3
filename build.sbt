import scalapb.compiler.Version.{grpcJavaVersion, scalapbVersion}


ThisBuild / version := "1"

ThisBuild / scalaVersion := "2.12.18"

lazy val root = (project in file("."))
  .settings(
    name := "hw3"
  )

libraryDependencies ++= Seq(
  "com.github.finagle" %% "finch-core" % "0.22.0",
  "com.github.finagle" %% "finch-circe" % "0.22.0",
  "io.circe" %% "circe-generic" % "0.9.0",
)

// https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.6"

// https://mvnrepository.com/artifact/com.typesafe/config
libraryDependencies += "com.typesafe" % "config" % "1.4.3"

// https://mvnrepository.com/artifact/org.scalameta/munit
libraryDependencies += "org.scalameta" %% "munit" % "1.0.2" % Test

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

libraryDependencies ++= Seq(
  "io.grpc" % "grpc-alts" % grpcJavaVersion,
//  "io.grpc" % "grpc-protobuf" % grpcJavaVersion,
  "io.grpc" % "grpc-netty" % grpcJavaVersion,
//  "io.grpc" % "grpc-stub" % grpcJavaVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapbVersion
)

// https://mvnrepository.com/artifact/software.amazon.awssdk/bedrock
libraryDependencies += "software.amazon.awssdk" % "bedrock" % "2.29.21"
// https://mvnrepository.com/artifact/software.amazon.awssdk/bedrockruntime
libraryDependencies += "software.amazon.awssdk" % "bedrockruntime" % "2.29.21"
// https://mvnrepository.com/artifact/software.amazon.awssdk/sso
libraryDependencies += "software.amazon.awssdk" % "sso" % "2.29.21"
// https://mvnrepository.com/artifact/software.amazon.awssdk/ssooidc
libraryDependencies += "software.amazon.awssdk" % "ssooidc" % "2.29.21"
// https://mvnrepository.com/artifact/com.amazonaws/aws-lambda-java-core
libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.2.3"
// https://mvnrepository.com/artifact/com.amazonaws/aws-lambda-java-events
libraryDependencies += "com.amazonaws" % "aws-lambda-java-events" % "3.14.0"

// play-json
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.14"

// https://mvnrepository.com/artifact/org.apache.httpcomponents.client5/httpclient5
libraryDependencies += "org.apache.httpcomponents.client5" % "httpclient5" % "5.4.1"


assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) =>
    xs match {
      case "MANIFEST.MF" :: Nil => MergeStrategy.discard
      case "services" :: _ => MergeStrategy.concat
      case _ => MergeStrategy.discard
    }
  case _ => MergeStrategy.first
}