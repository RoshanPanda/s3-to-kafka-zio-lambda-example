import com.roshan.config.AppConfig
import com.roshan.models.{Input, RecordDimensionObj}
import com.roshan.services.{S3Action, S3Service}
import org.scalatest.DoNotDiscover
import zio.{Console, ZIO, ZLayer}
import zio.s3._
import zio.stream.ZSink
import zio.test._

import java.net.URI

@DoNotDiscover
object s3AccessSpec extends ZIOSpecDefault {
  def spec: Spec[Any, Exception] =
    test("s3 connection test") {

    //val conf = AppConfig("","","","","","","","","","","","",0,"","","")
    val p = for {
      ls <- listBuckets
      r <- listBuckets.flatMap(a => listAllObjects(a.head.name,ListObjectOptions.from("test",5)).filterNot(s => s.key.endsWith("/")).runCollect)
      _ <- zio.Console.printLine(ls.map(o => o.name).mkString(","))
      _ <- zio.Console.printLine( r.map(_.key))
    } yield ls.length
    val uri = URI.create("http://localhost:9000")
    val layer = S3Service.s3LiveWithAccessKey("TESTKEY","TESTSECRET",Some(uri))
    val program = p.provide(layer)
    assertZIO(program)(Assertion.equalTo(2))
  }
}

@DoNotDiscover
object s3FileStreaming extends ZIOSpecDefault {
  def spec =
    test("s3 file streaming") {
      val conf = AppConfig("pe-bucket","","test","","","","","","","","","",0,"","","")

      val p = for {
        s3Serv <- ZIO.service[S3Action]
        records  <- s3Serv.listBucketL(conf)
          .flatMap(s3Serv.downloadKey)
          .via(Input.fromStringRecord)
          //.tap(a => Console.printLine(a))
          .map(a => a.map(b => RecordDimensionObj.fromInput(b)))
          .mapZIO(a => ZIO.collectAll(a))
          .tap(a => Console.printLine(s"test ${a}"))
          .run(ZSink.collectAll)

      } yield(records.length)

      val uri = URI.create("http://localhost:9000")
      val layer = S3Service.s3LiveWithAccessKey("TESTKEY", "TESTSECRET", Some(uri))
      val program = p.provide(layer,S3Service.s3ActionLive,ZLayer.succeed(conf))
      assertZIO(program)(Assertion.equalTo(0))
    }
}

@DoNotDiscover
object s3fileMove extends ZIOSpecDefault {
  def spec = test("test file move spec") {
    val conf = AppConfig("pe-test-2","archive/","test","","","","","","","","","",0,"","","")
    val p = for {
      s3Serv <- ZIO.service[S3Action]
      _ <- s3Serv.listBucketL(conf)
        .tap(a => Console.printLine(a.key))
        .mapZIO(a => s3Serv.archive(a.bucketName,a.key,"archive/"))
        .runDrain
      count <- s3Serv.listBucketL(conf).mapZIO(a => Console.printLine(a.key)).map(_ => 1).runSum
    } yield(count)
    val uri = URI.create("http://localhost:9000")
    val layer = S3Service.s3LiveWithAccessKey("TESTKEY", "TESTSECRET", Some(uri))

    val program = p.provide(layer,S3Service.s3ActionLive,ZLayer.succeed(conf))
    assertZIO(program)(Assertion.equalTo(0))
  }
}






