Feature: Rule

  A rule represent a part (or the whole) behaviour for a components.


  Scenario Outline: Join two filter rules
    Given I've two filter rules <ruleA>, <ruleB>
    When I join them togheter
    Then I obtain a result rule <ruleC> that's the <combination> of the two

    Examples:
      | ruleA                         | ruleB                                             | combination    | ruleC                                |
      | filter all the explored links |  return all <a href=""> links with a certain text | "union"        | unexplored links with a certain text |


  Scenario Outline: Join two policy rules
    Given I've two policy rules <ruleA>, <ruleB>
    When I join them togheter
    Then I obtain a result rule <ruleC> that's the <combination> of the two

    Examples:
      | ruleA                                | ruleB                                      | combination  | ruleC                                                                     |
      | document has a tag with a certain id |  document has a keyword on a specific path | "and"        | document with a tag with a certain id and that present a keyword in a path|
      | document has a tag with a certain id |  document has a keyword on a specific path | "or"         | document with a tag with a certain id or that present a keyword in a path |




