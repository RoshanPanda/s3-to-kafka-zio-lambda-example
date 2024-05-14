package com.roshan.services

import com.amazonaws.regions.Regions
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
import com.roshan.config.AppConfig
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer
import zio.json._
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.{UIO, ULayer, ZIO, ZLayer}

import java.util.Properties

case class MskSecret(username: String, password: String)

object MskSecret {
  implicit val encoder: JsonEncoder[MskSecret] = DeriveJsonEncoder.gen[MskSecret]
  implicit val decoder: JsonDecoder[MskSecret] = DeriveJsonDecoder.gen[MskSecret]
}


trait ApacheKafkaProducer[K, V] {
  def stop(): UIO[Unit]

  def sendToKafka(topic: String, key: K, value: V): ZIO[Any, Throwable, RecordMetadata]
}

object ApacheKafkaProducer {
  final case class ApacheKafkaService[K, V](kafka: KafkaProducer[K, V]) extends ApacheKafkaProducer[K, V] {
    override def stop(): UIO[Unit] = ZIO.succeed(kafka.flush()) *> ZIO.succeed(kafka.close())

    override def sendToKafka(topic: String, key: K, value: V): ZIO[Any, Throwable, RecordMetadata] = {
      val precord: ProducerRecord[K, V] = new ProducerRecord[K, V](topic, key, value)
      ZIO.attempt(kafka.send(precord).get())
    }
  }

  val layer: ZLayer[AppConfig with GetKafkaProp, Throwable, ApacheKafkaService[String, Array[Byte]]] = {
    val serv = for {
      settings <- ZIO.service[AppConfig]
      authStr <- ZIO.serviceWithZIO[GetKafkaProp].apply(_.getAuthString(settings.kafkaUserName, settings.SecretManger, settings.KafkaSecretKey))
      prop <- ZIO.serviceWithZIO[GetKafkaProp].apply(_.getKafkaProp(settings.Kserializer,settings.brokers.mkString(","), authStr))
    } yield {
      ApacheKafkaService[String, Array[Byte]](new KafkaProducer(prop))
    }
    val scope = ZIO.acquireRelease(serv)(_.stop())
    ZLayer.scoped(scope)
  }

  val layerLocal: ZLayer[GetKafkaProp, Throwable, ApacheKafkaService[String, Array[Byte]]] = {
    val serv = for {
      prop <- ZIO.serviceWithZIO[GetKafkaProp].apply(_.getKafkaPropLocal("localhost:29092"))
    } yield {
      ApacheKafkaService[String, Array[Byte]](new KafkaProducer(prop))
    }
    val scope = ZIO.acquireRelease(serv)(_.stop())
    ZLayer.scoped(scope)
  }

  def producerLayer(conf: AppConfig): ZLayer[Any, Throwable, Producer] =
    ZLayer.scoped(
      Producer.make(
        settings = ProducerSettings(conf.brokers.toList)
      )
    )

  def consumerLayer(conf: AppConfig): ZLayer[Any, Throwable, Consumer] =
    ZLayer.scoped(
      Consumer.make(
        settings = ConsumerSettings(conf.brokers.toList).withGroupId(conf.groupId)
      )
    )

}

trait GetKafkaProp {
  def getAuthString(username: String, secretName: String, secretKey: String): ZIO[Any, Throwable, String]

  def getKafkaProp(kserializer: String,bootstrap: String, authstring: String): ZIO[Any, Throwable, Properties]

  def getKafkaPropLocal(bootstrap: String):ZIO[Any, Throwable, Properties]
}

object GetKafkaProp {

  val live: ULayer[GetKafkaProp] = ZLayer.succeed {
    new GetKafkaProp {
      override def getAuthString(username: String, awsSecretName: String, secretKey: String): ZIO[Any, Throwable, String] = {
        val client = AWSSecretsManagerClientBuilder.standard.withRegion(Regions.US_EAST_1).build
        val request = new GetSecretValueRequest().withSecretId(awsSecretName)
        val response = client.getSecretValue(request)
        val parsedValue = response.getSecretString.fromJson[MskSecret]
        parsedValue match {
          case Right(value) => ZIO.succeed(s"org.apache.kafka.common.security.scram.ScramLoginModule required username=\"${username}\" password=\"${value.password}\";")
          case Left(e) => ZIO.fail(throw new Exception(s"failed while extracting secret for MSK with $e"))
        }
      }

      override def getKafkaProp(kserializer: String, bootstrap: String, authstring: String): ZIO[Any, Nothing, Properties] = {

        ZIO.succeed {
          val properties: Properties = new Properties
          properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getCanonicalName)
          properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer")
          properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap)
          properties.put("security.protocol", "SASL_SSL") //SASL_PLAINTEXT
          properties.put("sasl.mechanism", "SCRAM-SHA-512")
          properties.put("sasl.jaas.config", authstring)
          properties
        }
      }

      override def getKafkaPropLocal(bootstrap: String): ZIO[Any, Throwable, Properties] = {
        ZIO.succeed {
          val properties: Properties = new Properties
          properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getCanonicalName)
          properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer") //classOf[KafkaAvroSerializer].getCanonicalName)
          properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap)
          properties.put("schema.registry.url", "http://schema-registry:8081")
          properties
        }
      }
    }
  }

}