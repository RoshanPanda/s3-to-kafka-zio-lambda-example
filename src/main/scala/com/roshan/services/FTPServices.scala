package com.roshan.services

import zio._
import zio.ftp.SFtp.{ls, readFile, upload}
import zio.ftp._
import zio.stream.ZStream

import java.io.IOException
import scala.annotation.unused

 class FTPServices() {
  def list(path: String): ZIO[SFtp, Throwable, List[String]] =
    for {
      resources <- ls(path).runCollect
      res       <- ZIO.attempt(resources.map(_.path).toList)
    } yield res
  @unused
  def uploadSFTP(path: String, chunk: ZStream[Any, Throwable, Byte]): ZIO[SFtp with Any with Scope, IOException, Unit] = upload(path,chunk)
  def download(path: String, fileName:String): ZStream[SFtp, IOException, Byte] = readFile(path + "/" + fileName)
}

object FTPServices {
  def apply() = new FTPServices()
  val live: ZLayer[Any, Nothing, FTPServices] =  ZLayer.fromFunction( apply _)
}
