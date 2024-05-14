import com.roshan.models.RecordDimensionObj._
import com.roshan.config.AppConfig
import com.roshan.models.{Input, RecordDimensionObj}
import com.roshan.services.{ApacheKafkaProducer, GetKafkaProp, S3Action, S3Service}
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.{ZSink, ZStream}
import zio.test.{Assertion, ZIOSpecDefault, assertZIO}
import zio.{Chunk, Console, ZIO, ZLayer}

import java.net.URI
//kcat -b localhost:29092 -t dimension.promise.bay.v1 -p 3 -P
//kcat -C -b localhost:29092 -t dimension.promise.bay.v1

object KafkaProducerSpec extends ZIOSpecDefault{

  def spec = test("kafka producer") {
    val conf = AppConfig("pe-bucket","","test","dimension.promise.bay.v1","localhost:29092","localhost:29092","test-group","","","","","",0,"","","")

    val p = for {
      s3Serv <- ZIO.service[S3Action]
      kafkaServ <- ZIO.service[ApacheKafkaProducer[String, Array[Byte]]]
      records <- s3Serv.listBucketL(conf)
        .flatMap(s3Serv.downloadKey)
        .via(Input.fromStringRecord)
        .map(a => a.map(b => RecordDimensionObj.fromInput(b)))
        .mapZIO(a => ZIO.collectAll(a))
        .tap(a => Console.printLine(s"test ${a}"))
        .run(ZSink.collectAll)

      _ <- ZStream.fromIterable(records).mapZIO {
        case Some(a) => Console.printLine(a) *> kafkaServ.sendToKafka(conf.topic, a.itemId, a) *> Console.printLine(s"successfully inserted $a to kafka topic")
        case None => ZIO.unit
      }.run(ZSink.drain)

    } yield (records.length)

    val uri = URI.create("http://localhost:9000")
    val layer = S3Service.s3LiveWithAccessKey("TESTKEY", "TESTSECRET", Some(uri))
    val program = p.provide(layer,S3Service.s3ActionLive,ZLayer.succeed(conf),ApacheKafkaProducer.layerLocal,GetKafkaProp.live,ApacheKafkaProducer.consumerLayer(conf))
    assertZIO(program)(Assertion.equalTo(2))
  }
}

object kafkaConsumerSpec extends ZIOSpecDefault {
  def spec = test("consumer test") {
    val conf = AppConfig("pe-bucket","","test","dimension.promise.bay.v1","localhost:29092","localhost:29092","testgroup","","","","","",0,"","","")


    val p = for {
     // kafkaServ <- ZIO.service[ApacheKafkaProducer[String, Array[Byte]]]
      _ <- Console.printLine("started ")
      _ <- Consumer
        .plainStream(Subscription.topics("dimension.promise.bay.v1"), Serde.string, Serde.byteArray)
        .tap(r => Console.printLine(s"consuming ${r.key}"))
        .tap(r => Console.printLine(RecordDimensionObj.binaryCodec.decode(Chunk.fromArray(r.value)).map(_.itemId)))
        //.aggregateAsync(Consumer.offsetBatches)
        .mapZIO(r => r.offset.commit)
        .runDrain

    } yield()

    val program = p.provide(ApacheKafkaProducer.consumerLayer(conf))
    assertZIO(program)(Assertion.nothing)
  }
}
