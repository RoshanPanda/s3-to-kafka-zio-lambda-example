package com.roshan.util

import com.amazonaws.services.lambda.runtime.LambdaLogger
import zio.ZIO

object LoggingUtil {
  implicit class Log[A<:LambdaLogger](logger:A) {
    def info(input:String): ZIO[Any, Nothing, Unit] = ZIO.succeed(logger.log(input))
  }

}
