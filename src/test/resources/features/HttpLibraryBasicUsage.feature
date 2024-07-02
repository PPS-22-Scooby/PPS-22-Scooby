Feature: basic HTTP calls

  It's possible to make simple HTTP calls without too much of an hassle.

  Scenario Outline: Making a simple <requestType> request
    Given a simple <requestType> request
    And a URL "http://localhost:8080"
    When i make the HTTP call
    Then the returned content should be not empty

    Examples:
      | requestType   |
      | "GET"         |
      | "POST"        |

  Scenario Outline: Failing a simple <requestType> request
    Given a simple <requestType> request
    And a URL "https://nonesiste.com/fail"
    When i make the HTTP call
    Then it should return an error

    Examples:
      | requestType   |
      | "GET"         |
      | "POST"        |

  Scenario Outline: Obtaining headers for url <url>
    Given a simple <requestType> request
    And a URL <url>
    When i make the HTTP call
    Then the status code should be <statusCode> and the header content-type <contentType>

    Examples:
      | url                              | requestType   | statusCode | contentType                  |
      | "http://localhost:8080"          | "GET"          | 200        | "text/html; charset=UTF-8"  |
      | "http://localhost:8080/example"  | "GET"          | 404        | "text/html; charset=UTF-8" |
      | "http://localhost:8080/json"     | "POST"         | 200        | "application/json"          |
      | "http://localhost:8080"          | "POST"         | 404        | "text/html; charset=UTF-8" |