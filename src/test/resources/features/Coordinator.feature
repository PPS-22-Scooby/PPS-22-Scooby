Feature: Coordinator

  Scenario Outline: Page is already present in the crawled list
    Given I have a list of already crawled pages <crawled_pages>
    When I check if <page> is crawable
    Then The result of check should be false

    Examples:
      | crawled_pages | page |
      | http://www.google.it, https://www.unibo.it, http://www.wikipedia.com | https://www.unibo.it |

  Scenario Outline: Page is not present in the crawled list
    Given I have a list of already crawled pages <crawled_pages>
    When I check if <page> is crawable
    Then The result of check should be true

    Examples:
      | crawled_pages | page |
      | http://www.google.it, http://www.unibo.it, http://www.wikipedia.com  | http://www.ubisoft.com |


  Scenario Outline: Update the list of crawled pages
    Given I have a list of already crawled pages <crawled_pages>
    When I add <new_pages> to the crawled list
    Then The updated crawled list should be <updated_list>

    Examples:
      | crawled_pages                                    | new_pages                                    | updated_list                                                        |
      | http://www.google.it, http://www.unibo.it        | http://www.microsoft.com                     | www.google.it, www.unibo.it, www.microsoft.com |
      | http://www.wikipedia.com                         | http://www.wikipedia.com, http://www.sony.com| www.wikipedia.com, www.sony.com                |
      | http://www.google.it, http://www.wikipedia.com   | http://www.unibo.it                          | www.google.it, www.wikipedia.com, www.unibo.it |

  Scenario Outline: Validate URL before adding to the crawled list
    Given I have a list of already crawled pages <crawled_pages>
    When I add <new_pages> to the crawled list
    Then Only valid URLs should be added to the list, resulting in <updated_list>

    Examples:
      | crawled_pages                             | new_pages                              | updated_list                                   |
      | http://www.google.it, http://www.unibo.it | http://www.microsoft.com, invalid_url  | www.google.it, www.unibo.it, www.microsoft.com |
      | http://www.wikipedia.com                  | http://www.sony.com, www.invalid       | www.wikipedia.com, www.sony.com                |

  Scenario: Handle empty list of crawled pages
    Given I have an empty list of already crawled pages
    When I check if www.google.it is crawable
    Then The result of check should be false

  Scenario Outline: Avoid duplicates in the crawled list
    Given I have a list of already crawled pages <crawled_pages>
    When I add <new_pages> to the crawled list
    Then The updated crawled list should not contain duplicates and be <updated_list>

    Examples:
      | crawled_pages                             | new_pages                                      | updated_list                                       |
      | http://www.google.it, http://www.unibo.it | http://www.google.it, http://www.microsoft.com | www.google.it, www.unibo.it, www.microsoft.com     |
      | http://www.wikipedia.com                  | http://www.wikipedia.com, http://www.sony.com  | www.wikipedia.com, www.sony.com                    |

  Scenario Outline: Avoiding duplicates with different protocols
    Given I have a list of already crawled pages <crawled_pages>
    When I add <new_pages> to the crawled list
    Then The updated crawled list should not contain duplicates and be <updated_list>

    Examples:
      | crawled_pages                                   | new_pages                                         | updated_list                      |
      | http://www.example.com, https://www.example.com | http://www.example.com, https://www.example.com   | www.example.com                   |
      | http://www.wikipedia.com, http://www.google.com | https://www.wikipedia.com, https://www.google.com | www.wikipedia.com, www.google.com |


