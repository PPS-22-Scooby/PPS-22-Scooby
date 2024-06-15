Feature: Coordinator

  Scenario Outline: Crawler requests the ability to scrape pages
    Given I have a list of <pages>
    When Crawler requests the ability to scrape pages
    Then They should return a map of boolean

    Examples:
      | pages |
      | www.google.it, www.unibo.it, www.wikipedia.com  |
      | www.microsoft.com, www.sony.com |
      | www.test.it |

  Scenario Outline: Page is already present in the crawled list
    Given I have this <pages> are already crawled
    When I check if <page> is already crawled
    Then The result should be true

    Examples:
      | pages | page |
      | www.google.it, www.unibo.it, www.wikipedia.com | www.unibo.it |

  Scenario Outline: Page is not present in the crawled list
    Given I have this <pages> are already crawled
    When I check if <page> is already crawled
    Then The result should be false

    Examples:
      | pages | page |
      | www.google.it, www.unibo.it, www.wikipedia.com  | www.ubisoft.com |