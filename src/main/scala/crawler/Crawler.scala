package org.unibo.scooby
package crawler

trait DataAcquisition:
  def exploreUrl(url: String): Either[String, String]


class Crawler(val seed: String):
  def crawl(): Unit = ???
