package com.roshan.services

import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification
import com.amazonaws.services.s3.AmazonS3
import zio._
import zio.ftp.SFtp
import zio.s3.S3
import zio.stream.{ZSink, ZStream}
import com.roshan.util.LoggingUtil._
import com.roshan.config.AppConfig
import com.roshan.messages.RecordDimension
import com.roshan.models.{Input, RecordDimensionObj}
import RecordDimensionObj._

import scala.annotation.unused

object ProcessFlow {

  @unused
  def transformToKafka(config:AppConfig, kafkaServ:ApacheKafkaProducer[String,RecordDimension],s3Serv:S3Action): ZIO[S3 with ApacheKafkaProducer[String, RecordDimension] with S3Action, Serializable, Unit] =
    for {
      inputs    <- s3Serv.listBucketL(config)
        .flatMap(s3Serv.downloadKey)
        .via(Input.fromStringRecord)
        .tap(a => Console.printLine(a))
        .map(a => a.map(b => RecordDimensionObj.fromInput(b)))
        .mapZIO(a => ZIO.collectAll(a))
        .run(ZSink.collectAll)

      _       <- ZStream.fromIterable(inputs).mapZIO {
        case Some(a) => kafkaServ.sendToKafka(config.topic,a.itemId,a)//Producer.produce(config.topic,a.itemId,a.toString,Serde.string,Serde.string)
        case None => ZIO.unit
      }.run(ZSink.drain)


    } yield ()


  def transformToKafkaLambda(events: List[S3EventNotification.S3Entity],
                             logger:LambdaLogger,
                             config:AppConfig,
                             kafkaServ:ApacheKafkaProducer[String,Array[Byte]],
                             s3Serv:S3Action): ZIO[AmazonS3, Serializable, Unit] = {
    for {
      inputs <- ZStream.fromIterable(events)
        .flatMap(s3Serv.getObjectDefault)
        .via(Input.fromStringRecord)
        .tap(a => logger.info(s"input ${a}"))
        .map(a => a.map(b => RecordDimensionObj.fromInput(b)))
        .mapZIO(a => ZIO.collectAll(a))
        .tap(a => logger.info(s"transformed record ${a}"))
        .run(ZSink.collectAll)
      _ <- ZStream.fromIterable(inputs).mapZIO {
        case Some(a) => kafkaServ.sendToKafka(config.topic, a.itemId, a) *>
          logger.info(s"successfully inserted ${a.itemId} to kafka topic")
        case None => ZIO.unit
      }.run(ZSink.drain)

      _  <- ZStream.fromIterable(events)
        .flatMap( e => s3Serv.archiveDefault(e.getBucket.getName,e.getObject.getKey,config.archivePrefix)).run(ZSink.drain)
    } yield()
  }

  @unused
  def sftpUploadToS3(config: AppConfig,s3Serv:S3Service,sftpService:FTPServices): ZIO[S3 with SFtp, Throwable, Unit]
  = {
    for {
      _ <- ZIO.logInfo("Starting sftp job to get the file")
      ls           <- sftpService.list(config.FTPPath)
      _            <- ZIO.logInfo(s"files in path ${config.FTPPath} are ${ls.mkString(",")}")
      _            <- ZStream.fromIterable(ls).mapZIO {
        f => s3Serv.uploadFileFromSFTP(sftpService.download(config.FTPPath,f),f) *>
          ZIO.logInfo(s"upload successful for $f")
      }.runDrain
    } yield()
  }


}
