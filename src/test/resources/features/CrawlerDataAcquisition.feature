Feature: Crawler data acquisition.

  For making its operations a crawler need to get the data from
  the url.

  Data can be presented in various format like HTML, JSON, XML, etc, etc

  Rule: A crawler will skip invalid URLs.

    Example: An unresponsive URL
      Given an URL of an offline website
      When a crawler tries to fetch data from it
      Then will notice the user that the url can't be parsed and continues with other urls

  Rule: The type supported by the crawler should be only textual data

    Example: Download a video
      Given a user Fred that want to crawl a video url
      And the url will return the Content-Type header video/webm
      When it will start crawling
      Then will notice the user that the url can't be parsed because is not a text file




