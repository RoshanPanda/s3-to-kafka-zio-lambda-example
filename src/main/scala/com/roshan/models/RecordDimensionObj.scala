package com.roshan.models

import com.roshan.messages.RecordDimension
import zio.{IO, ZIO}
import zio.schema._
import zio.schema.codec.{AvroCodec, BinaryCodec}

object RecordDimensionObj {
  def apply(
      itemId: String,
      orgId: String,
      height: Option[Double],
      width: Option[Double],
      length: Option[Double],
      volume: Option[Double],
      dimensionUomId: Option[String],
      volumeUomId: Option[String],
      weight: Option[Double],
      weightUomId: Option[String]
  ): RecordDimension =
    new RecordDimension(
      itemId,
      orgId,
      height,
      width,
      length,
      volume,
      dimensionUomId,
      volumeUomId,
      weight,
      weightUomId
    )

  implicit val schema: Schema[RecordDimension] = DeriveSchema.gen
  implicit val transform: Schema[RecordDimension] = Schema[Input].transform(
    in =>
      new RecordDimension(
        in.mandatoryFields.itemId,
        in.mandatoryFields.orgId,
        in.height.map(_.toDouble),
        in.width.map(_.toDouble),
        in.length.map(_.toDouble),
        in.volume.map(_.toDouble),
        in.dimensionUomId,
        in.volumeUomId,
        in.weight.map(_.toDouble),
        in.weightUomId
      ),
    prd =>
      Input(
        MandatoryFields(prd.itemId, prd.orgId),
        prd.dimensionUom,
        prd.height.map(_.toString),
        prd.length.map(_.toString),
        prd.width.map(_.toString),
        prd.volumeUom,
        prd.volume.map(_.toString),
        prd.weightUOM,
        prd.weight.map(_.toString)
      )
  )

  def fromInput(in: Input): IO[String, RecordDimension] = {
    ZIO.fromEither(
      Input.schema.migrate(transform).flatMap(_(in))
    )
  }
  implicit val binaryCodec: BinaryCodec[RecordDimension] = AvroCodec.schemaBasedBinaryCodec(schema)
  implicit def encoding(input:RecordDimension)(implicit codec:BinaryCodec[RecordDimension]): Array[Byte] = {
    codec.encode(input).toArray
  }

}
