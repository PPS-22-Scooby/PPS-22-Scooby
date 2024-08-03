Feature: Scraper data filtering.

  Scenario: Successfully apply the policy
    Given I have a scraper with a proper configuration
    And   I have a document to apply policy to
    When  The scraper applies the policy
    Then  It should send the result

  Scenario: No matching after data filtering
    Given I have a scraper with a proper configuration
    And   I have a document with no matching
    When  The scraper applies the policy
    Then  It should send an empty result

  Scenario Outline: Different filtering strategies should be supported.
    Given I have a scraper with <by> filtering strategy and <parameters> selectors
    And   I have the following document as string
    """
      <html lang="en">
      <ul>
          <li class=testClass1>cl1</li>
          <li class=testClass2>cl2</li>
          <li id=testId1>cl3 testRegex</li>
          <li id=testId2>About</li>
        </ul>
      </html>
    """
    When  The scraper applies the policy
    Then  The scraper should obtain <results> as result
    Examples:
      |   by    |           parameters           |                                                                      results                                                                         |
      |   id    |       ["testId1", "testId2"]   |                                    ["<li id=\"testId1\">cl3 testRegex</li>", "<li id=\"testId2\">About</li>"]                                        |
      |   tag   |           ["li", "p"]          | ["<li class=\"testClass1\">cl1</li>", "<li class=\"testClass2\">cl2</li>", "<li id=\"testId1\">cl3 testRegex</li>", "<li id=\"testId2\">About</li>"] |
      |  class  |  ["testClass1", "testClass2"]  |                                    ["<li class=\"testClass1\">cl1</li>", "<li class=\"testClass2\">cl2</li>"]                                        |
      |   css   | [".testClass1", ".testClass2"] |                                    ["<li class=\"testClass1\">cl1</li>", "<li class=\"testClass2\">cl2</li>"]                                        |
      |  regex  |         ["testRegex"]          |                                                                  ["testRegex"]                                                                       |
