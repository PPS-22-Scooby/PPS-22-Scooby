Feature: Result update and aggregation

  Scenario: Successfully update result with a new entry
    Given I have an empty result
    When  The scraper obtain a new entry and update the result
    Then  The result should be updated with the new entry

  Scenario: Successfully update result with a new entry
    Given I have a non empty result
    When  The scraper obtain a new entry and update the result
    Then  The result should be updated with the new entry

  Rule: Result should support adding data

    Scenario Outline: Update result with a new entry
      Given I have a <partial_result> result of <type> type
      When I batch a new <add_data> entry
      Then The result should be <result> one
      Examples:
        |           type            |             partial_result            |                 add_data                  |                                 result                                       |
        |         String            |         <a>txt_match_filter</a>       |         <a>txt2_match_filter</a>          |          ["<a>txt_match_filter</a>", "<a>txt2_match_filter</a>"]             |
        | Map[String, String]       | {"a":"<a>txt_match_filter</a>"}    | {"div":"<div>txt_match_filter</div>"}  |    {"a":"<a>txt_match_filter</a>", "div":"<div>txt_match_filter</div>"}    |
        |         String            |     <a>match</a><div>match</div>      |           <div>match2</div>               |           ["<a>match</a><div>match</div>", "<div>match2</div>"]              |

    Scenario Outline: Update result with a multiple entry
      Given I have a <partial_result> result of <type> type
      When I batch new <add_data> entries
      Then The result should be <result> one
      Examples:
        |           type            |             partial_result            |                 add_data                  |                                 result                                       |
        |         String            |         <a>txt_match_filter</a>       |         ["<a>txt2_match_filter</a>", "prova"]          |          ["<a>txt_match_filter</a>", "<a>txt2_match_filter</a>", "prova"]             |
        | Map[String, String]       | {"a":"<a>txt_match_filter</a>"}    | {"a":"prova", "div":"<div>txt_match_filter</div>"}  |    {"a":"<a>txt_match_filter</a>, prova", "div":"<div>txt_match_filter</div>"}    |
        |         String            |     <a>match</a><div>match</div>      |           ["<div>match2</div>", "aaa"]               |           ["<a>match</a><div>match</div>", "<div>match2</div>", "aaa"]              |

  Rule: Result should support different types, such as String and Map

    Scenario Outline: Gather data in various format
      Given I have Scrapers that elaborate documents with different filtering policies which generates different data <type>
      And I have <documents> as document
      When The scraper starts filtering the document, obtaining data to aggregate
      Then It will obtain <result> as result

      Examples:
        |           type            |                   documents                              |              result              |
        |         String            | <a>txt_match_filter</a><a>txt2_match_filter</a>   |    ["<a>txt_match_filter</a><a>txt2_match_filter</a>"] |
        | Map[String, String]       | <a>txt_match_filter</a><div>txt_match_filter</div>   |    {"a":"<a>txt_match_filter</a>", "div":"<div>txt_match_filter</div>"}     |
        |         String            | <a>match</a><div>non_match</div><div>match</div>         |  ["<a>match</a><div>match</div>"]  |

  Rule: Result should be aggregated

    Scenario Outline: Aggregate different results
      Given I have 2 Scrapers that elaborate documents with the same filtering policies of <type>, which works on different documents
      When The scrapers finished, generated different <results> as result
      Then They will aggregate partial results obtaining <aggregate> as result

      Examples:
        |           type            |                                                 results                                                                   |                   aggregate                              |
        |         String            |                     ["<a>txt_match_filter</a>", "<a>txt2_match_filter</a>"]                                                   | ["<a>txt_match_filter</a>", "<a>txt2_match_filter</a>"]   |
        | Map[String, String]       | [{"a":"<a>txt_match_filter</a>", "div":"<div>txt_match_filter</div>"},{"a":"", "p":"<p>match</p>", "div":"match"}]  | {"a":"<a>txt_match_filter</a>", "div":"<div>txt_match_filter</div>, match", "p":"<p>match</p>"}   |
