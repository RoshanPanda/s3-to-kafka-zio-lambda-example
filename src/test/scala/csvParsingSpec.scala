import com.roshan.models.{Input, RecordDimensionObj}
import zio._
import zio.stream.{ZPipeline, ZSink, ZStream}
import zio.test._

import java.net.{URI, URL}
import scala.io.Codec

object csvParsingSpec extends ZIOSpecDefault {

  def spec = test("streaming csv file") {
    val fileurl1: URI = this.getClass.getClassLoader.getResource("test.csv").toURI
    val source = ZStream.fromFileURI(fileurl1)
      .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
      .drop(1)
      .via(Input.fromStringRecord)
      .map(a => a.map(b => RecordDimensionObj.fromInput(b)))
      .mapZIO(a => ZIO.collectAll(a))
      .tap(a => Console.printLine(a))
      .run(ZSink.collectAll)

    assertZIO(source.map(_.length))(Assertion.equalTo(36))
  }

}
