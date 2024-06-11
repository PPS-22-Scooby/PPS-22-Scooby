package org.unibo.scooby
package exporter

import io.cucumber.datatable.DataTable

import scala.annotation.targetName

object DummyExporter:

  class Exporter:
    
    @targetName("consumeResult")
    infix def <--(result: DataTable): Exporter = consume(result)
    
    def consume(result: DataTable): Exporter = this
    
    def exportAsString: String = ""

  trait ExportStrategy extends Exporter:

    override def consume(result: DataTable): Exporter =
      super.consume(result)
      publish(result)

    def publish(result: DataTable): Exporter

  trait NothingStrategy extends ExportStrategy:
    override def publish(result: DataTable): Exporter = this

    override def exportAsString: String =
      s"""
      ${super.exportAsString}
      ## NOTHING STRATEGY:

      I export nothing

      """.trim
      
  trait ListStrategy extends ExportStrategy:

    override def publish(result: DataTable): Exporter = this
  
  trait CsvStrategy extends ExportStrategy:
    override def publish(result: DataTable): Exporter = this
