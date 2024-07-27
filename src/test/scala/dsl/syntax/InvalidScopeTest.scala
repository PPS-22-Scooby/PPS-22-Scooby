package org.unibo.scooby
package dsl.syntax

import dsl.ScoobyEmbeddable

import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class InvalidScopeTest extends AnyFlatSpec, ScoobyEmbeddable, Matchers, BeforeAndAfterEach:

  "Outer scope keywords" should "be checked against recursive usage" in:
    "scooby {crawl {???}}" should compile
    "scooby {config {???}}" should compile
    "scooby {scrape {???}}" should compile
    "scooby {exports {???}}" should compile
    "scooby {crawl { crawl {???}}}" shouldNot compile
    "scooby {config { config {???}}}" shouldNot compile
    "scooby {scrape {scrape {???}; ???}}" shouldNot compile
    "scooby {exports {exports {???} ; ???}}" shouldNot compile

  "Config inner scope keywords" should "be checked against recursive usage" in:
    "scooby {config {network {???}}}" should compile
    "scooby {config {network {network {???}}}}" shouldNot compile
    "scooby {config {network {headers {???}}}}" should compile
    "scooby {config {network {headers { headers {???}}}}}" shouldNot compile
    "scooby {config {options {???}}}" should compile
    "scooby {config {options { option {???}}}}" shouldNot compile

  "Crawl inner scope keywords" should "be checked against recursive usage" in:
    "scooby {crawl {policy {???}}}" should compile
    "scooby {crawl {policy { policy {???}}}}" shouldNot compile

  "Exports inner scope keywords" should "be checked against recursive usage" in:
    "scooby {exports {batch {???}}}" should compile
    "scooby {exports {batch { Batch {???}}}}" shouldNot compile
    "scooby {exports {streaming {???}}}" should compile
    "scooby {exports {streaming { Streaming {???}}}}" shouldNot compile
    "scooby {exports {batch { strategy {???}}}}" should compile
    "scooby {exports {batch { strategy { strategy {???}}}}}" shouldNot compile



