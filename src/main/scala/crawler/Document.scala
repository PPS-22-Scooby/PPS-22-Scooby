package org.unibo.scooby
package crawler

trait Parser

trait Document:
  def url: URL
  def docType: String
  def content: String

class CrawlDocument:
  ???


trait ScrapeDocument:
  ???

object Document
