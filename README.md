# PPS-22-Scooby 🔍

## Team:

👨‍💻 Giovanni Antonioni - giovanni.antonioni2@studio.unibo.it

👨‍💻 Valerio Di Zio - valerio.dizio@studio.unibo.it

👨‍💻 Francesco Magnani - francesco.magnani14@studio.unibo.it

👨‍💻 Luca Rubboli - luca.rubboli2@studio.unibo.it

## Technologies:

🔄 Scrum

🛠 SBT

🔗 Git

🎯 YouTrack

🚀 Github Actions

## Overview:

PPS-22-Scooby is a web scraping and crawling application. It enables users to extract data from web pages by crawling through links and scraping specific content according to predefined rules.

## Features:

🕷 **Crawling**: The application navigates web pages, follows links, and retrieves content.

🔍 **Scraping**: Relevant data is extracted from HTML/XML pages using XPath, CSS selectors, or regular expressions.

🛠 **Customization**: Users can define custom scraping and crawling rules to suit their specific needs.

⚙️ **Parallel Processing**: Aspects of parallel programming are integrated for efficient execution.

📤 **Export**: Users can export extracted data in various formats according to their preferences.

## Implementation:

PPS-22-Scooby is built using Scala with Actor libraries for concurrency management. The application utilizes Git for version control, YouTrack for project management, and Github Actions for continuous integration.

## Get Started:

To start using Scooby from a new SBT project you need to manually add the library.
- start generating the new project using SBT;
- download the latest release of the Scooby library;
- create a new `lib` folder inside your sbt project;
- place the downloaded jar inside the `lib` folder you've just created;
- create a class that extends either `org.unibo.scooby.dsl.ScoobyEmbeddable` or `org.unibo.scooby.dsl.ScoobyApplication`


To import the application in a different scala file, all is required is

```Scala
import org.unibo.scooby.Application.scooby
import org.unibo.scooby.dsl.ScoobyApplication

object MyObject extends ScoobyApplication:

  scooby:
    config:
      network:
        Timeout is 9.seconds
        MaxRequests is 10
        headers:
          "User-Agent" to "Scooby/1.0-alpha (https://github.com/PPS-22-Scooby/PPS-22-Scooby)"
      options:
        MaxDepth is 2
        MaxLinks is 20
    
    crawl:
      url:
        "https://www.myTestUrl.com"
      policy:
        hyperlinks not external
    scrape:
      elements
    exports:
      batch:
        strategy:
          results get(el => (el.tag, el.text)) output:
            toFile("test.json") withFormat json
        aggregate:
          _ ++ _
      streaming:
        results get tag output:
          toConsole withFormat text
```

## Customization

Provided DSL is open to customization, we offer a brief introduction to explore possible configurations.

### Network

In order to enlarge visit to websites which require user authentication, it is possible to define multiple headers in
headers section as

```Scala
headers:
  "my-header-name-1" to "my-header-value-1"
  "my-header-name-2" to "my-header-value-2"
```

### Crawler

It is possible to define custom policies, which must adhere to type ```CrawlDocument ?=> Iterable[URL]```.
An example could be:
```Scala
policy:
  allLinks not external
```

### Scraper

It is possible to define custom policies, which must adhere to type ```ScrapeDocument ?=> Iterable[T]```.
It is also possible to mix policies using boolean filter conditions.
An example could be:

```Scala
scrape:
  elements that :
    haveAttributeValue("href", "level1.1.html") and haveClass("amet") or followRule {
      element.id == "ipsum"
    }
```

### Exporter

It is possible to define both batch and streaming strategies, even multiple times, concatenating their effects.
An example could be:

```Scala
exports:
  batch:
    strategy:
      results get(el => (el.tag, el.text)) output:
        toFile("testJson.txt") withFormat json
    
    aggregate:
      _ ++ _
  batch:
    strategy:
      results get(el => (el.tag, el.text)) output:
        toFile("testText.txt") withFormat text

    aggregate:
      _ ++ _
  streaming:
    results get tag output:
      toConsole withFormat text
```

When output is configured toFile, it's possible to define preferred file action, between Append (append results to
already existing text in file) and Overwrite (which delete previous content of the file).
Default behavior if not specified is Overwrite.

```Scala
exports:
  batch:
    strategy:
      results get(el => (el.tag, el.text)) output:
        toFile("testJson.txt", Append) withFormat json
    
    aggregate:
      _ ++ _
  batch:
    strategy:
      results get(el => (el.tag, el.text)) output:
        toFile("testText.txt", Overwrite) withFormat text

    aggregate:
      _ ++ _
  streaming:
    results get tag output:
      toConsole withFormat text
```
