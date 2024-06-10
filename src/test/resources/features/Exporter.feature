Feature: Exporter

  Scenario: Empty result
    Given I have an Exporter
    And an empty result
    When I try to export it
    Then it should return ''

  Scenario: Result to csv
    Given I have an Exporter with a csv strategy
    And the following result
      | tag      | amount |
      | <a>      | 10     |
      | <button> | 20     |
    When I try to export it
    Then it should return 'tag;amount\n<a>;10\n<button>;20'


