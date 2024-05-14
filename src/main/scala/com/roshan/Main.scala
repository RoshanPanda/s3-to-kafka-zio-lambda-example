package com.roshan

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.roshan.util.LoggingUtil._
import com.roshan.config.AppConfig
import com.roshan.services.{ApacheKafkaProducer, GetKafkaProp, ProcessFlow, S3Action, S3Service}
import zio._

import scala.jdk.CollectionConverters.CollectionHasAsScala


/*object Main extends ZIOAppDefault {

  val app: ZIO[Any, Serializable, Unit] = {
    for {
    config <- ZIO.service[AppConfig]
    s3Serv <- ZIO.service[S3Action]
    kafkaServ <- ZIO.service[ApacheKafkaProducer[String, PromisingRecordDimension]]
    _ <- ProcessFlow.transformToKafka(config,kafkaServ,s3Serv).schedule(Schedule.fixed(10.minutes))
  } yield()
  }.provide(S3Service.s3Live,ApacheKafkaProducer.layer,S3Service.s3ActionLive,AppConfig.live,GetKafkaProp.live,Scope.default,ZLayer.Debug.mermaid)

  def run: ZIO[Any, Serializable, Nothing] = app.forever
}*/

object Main extends RequestHandler[S3Event,String] {
  override def handleRequest(input: S3Event, context: Context): String = {
    val app = {
      for {
        logger <- ZIO.succeed(context.getLogger)
        _       <- logger.info("starting lambda execution")
        config <- ZIO.service[AppConfig]
        s3Serv <- ZIO.service[S3Action]
        kafkaServ <- ZIO.service[ApacheKafkaProducer[String, Array[Byte]]]
        _       <- logger.info(s"received s3 events ${input.getRecords.asScala.mkString(",")}")
        events <- ZIO.succeed(input.getRecords.asScala.toList.map(_.getS3))
        _ <- ProcessFlow.transformToKafkaLambda(events,logger,config, kafkaServ, s3Serv)
      } yield ()
    }.provide(
      S3Service.s3Live,
      ApacheKafkaProducer.layer,
      S3Service.s3ActionLive,
      AppConfig.live,
      GetKafkaProp.live,
      Scope.default,
      ZLayer.Debug.mermaid,
      S3Service.defaultClient
    )
    Unsafe.unsafe { implicit unsafe =>
      zio.Runtime.default.unsafe.run(
        ZIO.succeed("starting lambda") *> app *> ZIO.succeed("Lambda execution completed")
      ).getOrThrowFiberFailure()
    }
  }
}

