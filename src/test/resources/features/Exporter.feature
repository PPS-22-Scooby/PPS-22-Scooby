Feature: Exporter

  Scenario Outline: Empty result
    Given I have an Exporter with a <format> strategy
    And the following result
      |
    When I try to export it
    Then it should return <expectedOutput>

    Examples:
      | format      | expectedOutput |
      | "listCount"      | '[]'           |
      | "csvCount"       | ''             |

  Scenario Outline: Result to <format>
    Given I have an Exporter with a <format> strategy
    And the following result
      | tag      | amount |
      | <a>      | 10     |
      | <button> | 20     |
    When I try to export it
    Then it should return <expectedOutput>

    Examples:
      | format           | expectedOutput                    |
      | "listCount"      | '[[a,10],[button,20]]'            |
      | "csvCount"       | 'tag;amount\n<a>;10\n<button>;20' |

  Scenario Outline: Result to stream with <format>
    Given I have an Exporter with a <format> strategy
    And the following result of many
      | tag      | amount |
      | <a>      | 10     |
    And the following result of many
      | tag      | amount |
      | <button> | 20     |
    When I try to export it
    Then it should return first <expectedOutput1> and then <expectedOutput2>

    Examples:
      | format           | expectedOutput1      | expectedOutput2                   |
      | "listCount"      | '[[a,10]]'           | '[[a,10],[button,20]]'            |
      | "csvCount"       | 'tag;amount\n<a>;10' | 'tag;amount\n<a>;10\n<button>;20' |



