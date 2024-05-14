package com.roshan.config

import zio.Config
import zio._
import zio.ftp.{ConnectionError, FtpCredentials, SFtp, SecureFtpSettings, secure}

case class AppConfig(bucketName: String,
                     archivePrefix:String,
                     prefix:String,
                     topic:String,
                     broker1:String,
                     broker2:String,
                    // broker3:String,
                     groupId:String,
                     kafkaUserName:String,
                     SecretManger:String,
                     KafkaSecretKey:String,
                     Kserializer:String,
                     FTPh:String = "",
                     FTPp:Int = 0,
                     FTPu:String = "",
                     FTPpass:String = "",
                     FTPPath:String = "") {
  val brokers: Seq[String] = List(broker1,broker2)


}

/**
 *
 */
object AppConfig {
  val config: Config[AppConfig] = {
    (Config.string("BUCKET_NAME")
      ++ Config.string("ARCHIVE_PREFIX")
      ++ Config.string("FILE_PREFIX")
      ++ Config.string("TOPIC_NAME")
      ++ Config.string("BROKER_1")
      ++ Config.string("BROKER_2")
     // ++ Config.string("BROKER_3")
      ++ Config.string("CONSUMER_GROUP_ID")
      ++ Config.string("MSK_USER_NAME")
      ++ Config.string("MSK_SECRET_MANAGER_NAME")
      ++ Config.string("KAFKA_PASSWORD_KEY")
      ++ Config.string("MSK_SERIALIZER")
      /*++ Config.string("FTP_HOST")
      ++ Config.int("FTP_PORT")
      ++ Config.string("FTP_USER")
      ++ Config.string("FTP_PASSWORD")
      ++ Config.string("FTP_PATH")*/
      ).map {
      case (bucket, archiveP, prefix,topic, broker1, broker2,groupid,kuser,ksecman,kseckey,kserde) => AppConfig(bucket, archiveP,prefix,topic, broker1,broker2,groupid,kuser,ksecman,kseckey,kserde)

    }
  }

  val live: ZLayer[Any, Config.Error, AppConfig] = ZLayer.fromZIO(ZIO.config(config))
}

case class FtpConnection() {
  def get(conf: AppConfig): SecureFtpSettings = SecureFtpSettings(conf.FTPh, conf.FTPp, FtpCredentials(conf.FTPu, conf.FTPpass))
}
  object FtpConnection {
    def create: FtpConnection = FtpConnection()
    def live(conf:AppConfig): ZLayer[Scope, ConnectionError, SFtp] = secure(create.get(conf))
  }