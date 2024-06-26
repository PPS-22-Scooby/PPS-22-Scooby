package org.unibo.scooby
package core.exporter

import play.api.libs.json.{JsObject, JsValue, Json, Writes}

import java.io.PrintWriter
import scala.annotation.targetName
import scala.collection.mutable
import scala.reflect.ClassTag

case class Result[T] (data: Seq[T]):
  def +(other: Result[T]): Result[T] = Result(data concat other.data)

private type StreamConsumer[T] = Result[T] => Unit
private type BatchConsumer[T] = Result[T] => Unit

class Exporter[T] private(
                         onSingleResultConsumer: StreamConsumer[T],
                         onResultReadyConsumer: BatchConsumer[T],
                       ):
  val results: mutable.Seq[Result[T]] = mutable.Seq.empty[Result[T]]

  def updateStream(result: Result[T]): Unit =
    results :+ result
    onSingleResultConsumer(result)

  def onResultReady(): Unit =
    onResultReadyConsumer(results.fold(Result(Seq.empty))(_ + _))

object Exporter:

  given streamPrintConsole[T]: StreamConsumer[T] = result => println(result)
  given batchPrint[T]: BatchConsumer[T] = result => println(result)
  def batchToCsv[T]: String => BatchConsumer[T] =
    path =>
      results => writeStringToFile(path, resultAsCsv(results))

  private def extractHeaders[T: ClassTag](data: T): String =
    implicitly[ClassTag[T]].runtimeClass.getDeclaredFields.map(_.getName).mkString(",")

  private def resultAsCsv[T](result: Result[T])(implicit writer: Writes[T]): String =
    val headers = extractHeaders(result.data)
    val rows = result.data.map:
      Json.toJson(_).as[JsObject].values.mkString(",")
    (headers +: rows).mkString("\n")

  private def writeStringToFile(filePath: String, content: String): Unit =
    val writer = new PrintWriter(filePath)
    try {
      writer.write(content)
    } finally {
      writer.close()
    }

  given csvExporter[T]: BatchConsumer[T] = result =>
    val csv = resultAsCsv(result)

  def concatenate[T](consumer1: BatchConsumer[T], consumer2: BatchConsumer[T]): BatchConsumer[T] =
    result => consumer1(result); consumer2(result)

  def apply[T](using onSingleResultConsumer: StreamConsumer[T])(using onResultReadyConsumer: BatchConsumer[T]): Exporter[T] =
    new Exporter(onSingleResultConsumer, onResultReadyConsumer)
  
  @targetName("consumeResult")
  infix def <--[T](result: Result[T])(implicit exporter: Exporter[T]): Unit = {
    exporter.updateStream(result)
  }





