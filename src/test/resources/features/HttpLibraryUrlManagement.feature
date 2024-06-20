Feature: simple URL creation and management

  It's possible to create and manage URLs easily

  Scenario: creating a URL from string
    Given the string "https://www.example.com/"
    When i convert it to URL
    Then it should return a valid URL

  Scenario: invalid URL
    Given the string "//www.example.com/"
    When i convert it to URL
    Then it should result in a Malformed URL error

  Scenario: appending two URL
    Given two URLs "https://www.example.com/" and "/example"
    When i append them
    Then it should return the URL "https://www.example.com/example"

  Scenario: obtaining the domain
    Given the URL "https://www.example.com/"
    When i get the domain
    Then it should return "www.example.com"

  Scenario: navigating up
    Given the URL "https://www.example.com/example"
    When i go to the parent
    Then it should return an URL "https://www.example.com/"

  Scenario: comparing the depth
    Given two URLs two URLs "https://www.example.com/" and "https://www.example.com/example"
    When i compare them
    Then the first should be lower than the second





