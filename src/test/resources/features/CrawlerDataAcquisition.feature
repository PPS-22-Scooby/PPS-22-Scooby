Feature: Crawler data acquisition.

  For making its operations a crawler need to get the data from
  the url.

  Data can be presented in various format like HTML, JSON, XML, etc, etc

  Rule: A crawler will skip invalid URLs.

    Example: A non well-formatted URL
      Given a non well-formatted url
      When a crawler tries to check it for data
      Then will notice the user that the url can't be parsed and continues with other urls

    Example: An unresponsive URL
      Given an URL of an offline website
      When a crawler tries to fetch data from it
      And reach a connection timeout
      Then will notice the user that the url can't be parsed and continues with other urls

  Rule: The type supported by the crawler should be only textual data

    Example Download a video
      Given a user Fred that want to crawl the url https://www.youtube.com/watch?v=dQw4w9WgXcQ
      And the url will return the Content-Type header video/webm
      When it will start crawling
      Then will notice the user that the url can't be parsed and continues with other urls


    Scenario Outline: Gather data in various format
      Given a user Pino that navigate to an url
      And the url will return the Content-Type header <type>
      When it will start crawling
      Then will use the <strategyType> strategy for getting data

      Examples:
        | type       | strategyType |
        | text/html  | html         |
        | text/plain | plain        |
        | text/xml   | xml          |




