Feature: Exporter feature

  Scenario: Empty page scenario
    Given I have no content to export
    When I try to export it
    Then it should return an empty list
