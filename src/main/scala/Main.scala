import java.nio.file._
import java.io.File

import scala.util.Try

import org.apache.commons.io.IOUtils

import com.amazonaws.auth._
import com.amazonaws.auth.profile._
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3._
import com.amazonaws.services.s3.model._

object Main extends App {

  sealed trait Command
  case object Unspecified extends Command
  case object Upload extends Command
  case object Download extends Command
  
  case class Config(
    s3url: String = "",
    awsProfile: String = "",
    filename: String = "",
    kmsKeyId: String = "",
    region: String = "eu-west-1",
    profile: String = "",
    command: Command = Unspecified
  )

  val parser = new scopt.OptionParser[Config]("s3-encrypt") {
    opt[String]('r', "region")
      .action((r, cfg) => cfg.copy(region = r))
      .text("AWS region (default = eu-west-1)")

    opt[String]('p', "profile")
      .action((p, cfg) => cfg.copy(profile = p))
      .text("AWS credentials profile")

    cmd("upload")
      .action((_, cfg) => cfg.copy(command = Upload)) 
      .text("Encrypt and upload a file to S3")

    cmd("download")
      .action((_, cfg) => cfg.copy(command = Download)) 
      .text("Download a file from S3 and decrypt it")

    arg[String]("s3-url")
      .action((s, cfg) => cfg.copy(s3url = s))
      .text("S3 URL e.g. s3://my-bucket/my-file.txt")

    arg[String]("local-file")
      .action((f, cfg) => cfg.copy(filename = f))
      .text("""Local file path. Use "stdin" to upload from stdin, "stdout" to download to stdout""")

    arg[String]("kms-key-id")
      .action((k, cfg) => cfg.copy(kmsKeyId = k))
      .text("KMS master key ID")

    checkConfig(cfg => {
      if (Try(Regions.fromName(cfg.region)).isFailure)
        failure(s"Invalid AWS region: ${cfg.region}")
      else if (Try(new AmazonS3URI(cfg.s3url)).isFailure)
        failure(s"Invalid S3 URL: ${cfg.s3url}")
      else if (cfg.command == Unspecified)
        failure("Must specify whether you want to upload or download")
      else
        success
    })
  }

  def run(config: Config): Unit = {

    val client: AmazonS3EncryptionClient = {
      val region = Region.getRegion(Regions.fromName(config.region))
      val credentialsProvider = buildCredsProvider(config)
      val kmsMaterialsProvider = new KMSEncryptionMaterialsProvider(config.kmsKeyId)
      val cryptoConfig = new CryptoConfiguration().withAwsKmsRegion(region)
      new AmazonS3EncryptionClient(credentialsProvider, kmsMaterialsProvider, cryptoConfig)
          .withRegion(region)
    }

    config.command match {
      case Upload => upload(client, config)
      case Download => download(client, config)
      case _ =>
    }

  }

  private def upload(client: AmazonS3EncryptionClient, config: Config): Unit = {
    val s3uri = new AmazonS3URI(config.s3url)
    if (config.filename == "stdin")
      client.putObject(new PutObjectRequest(s3uri.getBucket, s3uri.getKey, System.in, new ObjectMetadata))
    else {
      client.putObject(new PutObjectRequest(s3uri.getBucket, s3uri.getKey, new File(config.filename)))
      println(s"Uploaded ${config.filename} to $s3uri") 
    }
  }

  private def download(client: AmazonS3EncryptionClient, config: Config): Unit = {
    val s3uri = new AmazonS3URI(config.s3url)
    val obj = client.getObject(s3uri.getBucket, s3uri.getKey)
    val bytes = IOUtils.toByteArray(obj.getObjectContent())
    if (config.filename == "stdout")
      System.out.write(bytes)
    else {
      Files.write(Paths.get(config.filename), bytes)
      println(s"Downloaded $s3uri to ${config.filename}") 
    }
  }

  private def buildCredsProvider(config: Config) = {
    if (config.profile.nonEmpty) {
      new AWSCredentialsProviderChain(
        new ProfileCredentialsProvider(config.profile),
        new EnvironmentVariableCredentialsProvider(),
        new InstanceProfileCredentialsProvider()
      )
    } else {
      new AWSCredentialsProviderChain(
        new EnvironmentVariableCredentialsProvider(),
        new InstanceProfileCredentialsProvider()
      )
    }
  }

  parser.parse(args, Config()).map(run)

}
