Feature: basic HTTP calls

  It's possible to make simple HTTP calls without too much of an hassle.

  Scenario: Making a simple GET request
    Given a simple GET request
    And a URL "https://www.example.com/"
    When i make the HTTP call
    Then the returned content should be not empty

  Scenario: Making a simple POST request
    Given a simple POST request
    And a URL "https://www.example.com/"
    When i make the HTTP call
    Then the returned content should be not empty

  Scenario: Failing a simple GET request
    Given a simple GET request
    And a URL "https://nonesiste.com/fail"
    When i make the HTTP call
    Then it should return in a error


Feature: obtaining responses metadata

  It's possible to obtain responses metadata (e.g. status, headers)

  Scenario Outline: Obtaining headers
    Given a simple <requestType> request
    And a URL <url>
    When i make the HTTP call
    Then the status code should be <statusCode> and the header "Content-Type" <contentType>

    Examples:
      | url                               | requestType |statusCode | contentType              |
      | https://www.example.com/          | GET         | 200       | text/html; charset=UTF-8 |
      | https://www.example.com/example   | GET         | 404       | text/html; charset=UTF-8 |
      | https://catfact.ninja/fact        | GET         | 200       | application/json         |
      | https://catfact.ninja/fact        | POST        | 404       | application/json         |

