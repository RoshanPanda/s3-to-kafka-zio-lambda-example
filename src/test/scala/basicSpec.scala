import com.roshan.models.{Input}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.io.Codec

class basicSpec extends AnyWordSpecLike {

  "test" in {
    val Str = "123456,BAY,3,5,8,12,CM,6"
    val out = Input.csvParse(Str)
    println(out)
    assert(true)
  }

}
