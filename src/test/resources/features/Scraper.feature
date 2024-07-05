Feature: Scraper data filtering.

  Rule: A scraper should be able to apply different filtering strategy.

  val classSelector: Seq[String] = Seq("testClass1", "testClass2")
  val idSelector: Seq[String] = Seq("testId1", "testId2")
  val cssSelector: Seq[String] = classSelector.map("." concat _)
  val tagSelector: Seq[String] = Seq("li", "p")
  val regEx: String = "testRegex"

    Scenario Outline: Scraping with different filtering rules
      Given a scraper with <by> filtering strategy with <parameters> params
      And   the following document as string
            """
            <html lang="en">
            <head>
              <title>Basic HTML Document</title>
            </head>
            <body>
              <header>
                <h1>Welcome to My Website</h1>
              </header>
              <nav>
                <ul>
                  <li><a href="#about">About</a></li>
                </ul>
              </nav>
              <main>
                <section id="contact">
                  <h2>Contact</h2>
                  <p>This is the contact section.</p>
                </section>
              </main>
              <footer>
                <p>&copy; 2024 My Website</p>
              </footer>
            </body>
            </html>
            """
      When  it filters the document
      Then  these <results> should be obtained
      Examples:
        |     by    |           parameters           | results              |
        |   "id"    |       ["testId1", "testId2"]   |                      |
        |   "tag"   |           ["li", "p"]          |                      |
        |  "class"  |  ["testClass1", "testClass2"]  |                      |
        |   "css"   | [".testClass1", ".testClass2"] |                      |
        |   "regex" |         "testRegex"            |                      |

    Example: Change the filtering strategy
      Given a scraper that hasn't be started yet
      When  we assign a new filtering strategy
      And   it starts scrape a document
      Then  it will scrape using the new strategy set
