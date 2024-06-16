Feature: Scraper data loading.

  The first step in data scraping is data loading. Given a document as string, should
  be provided a robust data structure to handle document decomposition considering scraper's filtering
  configurations.

  Scenario Outline: Empty given document
    Given a Scraper with a proper configuration
    And the following document
      |
    When I try to load it
    Then it should return <expectedOutput>

    Examples:
      | expectedOutput  |
      | ""              |

  Scenario Outline: Valid given document
    Given a Scraper with a proper configuration
    And   the following document as string
      """
        <html>
        <head>
        <title>Sample Title</title>
        </head>
        <body>
        <div id="content" class="main" data-info="example">
        <p>Hello, World!</p>
        <a>Hello, World! That's a prova</a>
        </div>
        <div id="content" class="main md" data-info="example">
        <p>Hello, World!</p>
        <a>Hello, World! That's a prova</a>
        </div>
        </body>
        </html>
      """
    When  I try to load it
    Then  it should return <expectedOutput> as structured document

    Examples:
      | expectedOutput  |
      """
        <html>
        <head>
        <title>Sample Title</title>
        </head>
        <body>
        <div id="content" class="main" data-info="example">
        <p>Hello, World!</p>
        <a>Hello, World! That's a prova</a>
        </div>
        <div id="content" class="main md" data-info="example">
        <p>Hello, World!</p>
        <a>Hello, World! That's a prova</a>
        </div>
        </body>
        </html>
      """

Feature: Scraper data filtering.

  After loading, deriving result has to be filtered following scraper's configurations.
  In this phase, any regular expressions and filtering rules provided are applied.

  Rule: A scraper should be able to apply different filtering strategy.

    Example: Scraping with different filtering rules
      Given a scraper with the following filtering strategies
        | strategy      |                           rule                              |
        | "selectors"   |                   '["id", "class"]'                         |
        | "regEx"       |             '["a" -> "prova", "div" -> "prova"]'            |
        | "attributes"  | '["a" -> ["id", "class"], "div" -> ["class", "data-info", "my-attr"]]' |
      And   the following document as string
            """
              <html>
              <head>
              <title>Sample Title</title>
              </head>
              <body>
              <div id="content" class="main" data-info="example" my-attr="example">
              <p>Hello, World!</p>
              <a>Hello, World! That's a prova</a>
              </div>
              <div id="content" class="main md" data-info="example">
              <p>Hello, World!</p>
              <a>Hello, World! That's a prova</a>
              </div>
              </body>
              </html>
            """
      # Multi lines values in table should be labelled like this SELECTORS_PLACEHOLDER and handled like
      # Then("""^these <results> should be obtained$""") { (dataTable: io.cucumber.datatable.DataTable) =>
      # val actualResults = dataTable.asMaps(classOf[String], classOf[String])
      # actualResults.foreach { row =>
      # val strategy = row.get("strategy")
      # val expectedResult = row.get("results") match {
      # case "SELECTORS_PLACEHOLDER" =>
      #   """<div id="content" class="main" data-info="example"><p>Hello, World!</p><a>Hello, World! That's a prova</a></div>
      #   |<div id="content" class="main md" data-info="example">
      #   |<p>Hello, World!</p>
      #   |<a>Hello, World! That's a prova</a>
      #   |</div>""".stripMargin
      # case other => other
      # }
      When  it filters the document
      Then  these <results> should be obtained
        | strategy      |                           results                                               |
        | "selectors"   |                   """<div id="content" class="main" data-info="example">
                                                  <p>Hello, World!</p>
                                                  <a>Hello, World! That's a prova</a>
                                                </div>
                                                <div id="content" class="main md" data-info="example">
                                                  <p>Hello, World!</p>
                                                  <a>Hello, World! That's a prova</a>
                                                </div>"""                                                 |
        | "regEx"       |             '["""<div id="content" class="main" data-info="example">
                                          <p>Hello, World!</p>
                                          <a>Hello, World! That's a prova</a>
                                        </div>""",
                                        """<a>Hello, World! That's a prova</a>""",
                                        """<div id="content" class="main md" data-info="example">
                                          <p>Hello, World!</p>
                                          <a>Hello, World! That's a prova</a>
                                        </div>"""]'                                                       |
        | "attributes"  | '["""<div id="content" class="main" data-info="example" my-attr="example">
                                <p>Hello, World!</p>
                                <a>Hello, World! That's a prova</a>
                              </div>"""]'                                                                 |

    Example: Change the filtering strategy
      Given a scraper that hasn't be started yet
      When  we assign a new filtering strategy
      And   it starts scrape a document
      Then  it will scrape using the new strategy set

Feature: Scraper result aggregation.

  Last phase involves data aggregation: all filtered data are collapsed in a data structure
  allowing a proper visualization.

  Rule: A scraper should encapsulate results in a proper data structure, enabling visualization.

    Example: Scraper finished the filtering phase encapsulate its results
      Given a scraper and a document to scrape
      When  after filtering phase
      Then  the scraper converts results in a data structure
