Feature: Coordinator

  Scenario Outline: Crawler requests the ability to scrape pages
    Given I have a list of <pages>
    When Crawler requests the ability to scrape pages
    Then They should return a map of boolean

    Examples:
      | pages |
      | www.google.it, www.unibo.it, www.wikipedia.com  |
      | www.microsoft.com, www.sony.com |
      | www.test.it   |

  Scenario: