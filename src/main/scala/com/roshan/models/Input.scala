package com.roshan.models


import zio.ZIO
import zio.prelude.Validation
import zio.prelude.ZValidation.{Failure, Success}
import zio.schema.{DeriveSchema, Schema}
import zio.stream.ZPipeline

case class MandatoryFields(itemId: String,
                               orgId: String)

case class Input(mandatoryFields:MandatoryFields,
                     dimensionUomId: Option[String],
                     height: Option[String],
                     length: Option[String],
                     width: Option[String],
                     volumeUomId: Option[String],
                     volume: Option[String],
                     weightUomId: Option[String],
                     weight: Option[String])



object Input {
  implicit val schema: Schema[Input] = DeriveSchema.gen

  def validateItemId(id:String): Validation[String, String] =
    if(id.isEmpty) Validation.fail("itemId is empty and ignoring record")
    else Validation.succeed(id)

  def validateorgId(id:String): Validation[String, String] =
    if (id.isEmpty) Validation.fail("itemId is empty and ignoring record")
    else Validation.succeed(id)

  def WMOSInputValidation(itemId:String,orgId:String): Validation[String,MandatoryFields] = {
    Validation.validateWith(validateItemId(itemId),validateorgId(orgId))(MandatoryFields)
  }


  def mkInput(itemId:String,
                  orgId: String,
                  dimensionUomId: Option[String],
                  height: Option[String],
                  length: Option[String],
                  width: Option[String],
                  volumeUomId: Option[String],
                  volume: Option[String],
                  weightUomId: Option[String],
                  weight: Option[String]
                 ):ZIO[Any,String,Option[Input]] = {

    WMOSInputValidation(itemId:String,orgId: String) match {
      case Success(_, value) => ZIO.succeed(Some(Input(value,dimensionUomId,height,length,width,volumeUomId,volume,weightUomId,weight)))
      case Failure(_, errors) => ZIO.fail(s"failed during mandatory validation ${errors.mkString("")}")

    }
  }

  def fromStringRecord: ZPipeline[Any, String, String, Option[Input]] = {
    ZPipeline[String].mapZIO(csvParse)
  }

  def csvParse(rec:String): ZIO[Any, String, Option[Input]] = {
    val keys = sourceKeys.values.toList
    val recArray = rec.split(",").map(_.trim).toList
    val recMap = keys.zip(recArray).toMap

    mkInput(recMap(sourceKeys.item_id),
      recMap(sourceKeys.org_id),
      recMap.get(sourceKeys.dimension_Uom_Id) match {
        case Some("") => None
        case b => b
      },
      recMap.get(sourceKeys.height) match {
        case Some("") => None
        case b => b
      },
      recMap.get(sourceKeys.length) match {
        case Some("") => None
        case b => b
      },
      recMap.get(sourceKeys.width) match {
        case Some("") => None
        case b => b
      },
      recMap.get(sourceKeys.volume_Uom_Id) match {
        case Some("") => None
        case b => b
      },
      recMap.get(sourceKeys.volume) match {
        case Some("") => None
        case b => b
      },
      recMap.get(sourceKeys.weight_Uom_Id) match {
        case Some("") => None
        case b => b
      },
      recMap.get(sourceKeys.weight) match {
        case Some("") => None
        case b => b
      }
    )
  }

}

object sourceKeys extends Enumeration {
  type sourceKeys = Value
  val item_id: Value = Value("itemId")
  val org_id = Value("orgId")
  val dimension_Uom_Id = Value("dimensionUomId")
  val height = Value("height")
  val length = Value("length")
  val width = Value("width")
  val volume_Uom_Id = Value("volumeUomId")
  val volume = Value("volume")
  val weight_Uom_Id = Value("weightUomId")
  val weight = Value("weight")
}

