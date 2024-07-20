package org.unibo.scooby
package dsl


/*
scooby:
  config:
    network:
      Timeout -> 1.seconds
    options:
      MaxDepth -> 3
      MaxLinks -> 10


    network:
      - NetworkTimeout(5.seconds)
      - MaxRequests(5)

  crawl:
    url:
      "https://www.prova.it"
    policy:
      links filter (_.contains("blabla"))

  scrape:
    policy:

    [ScrapeDocument, ...] + [..., ...] + [..., Iterable[T]]

    PolicyBuilder() + (_.getElements) + () build

    matchesOf("""class="gorgeous"""") ++ (elements withClass "gorgeous")

    (elements withClass "gorgeous").map(_.toString()) \
    elements withClass "gorgeous2"

    elements + _.withClass "gorgeous" + _.map(_.toSTring())

    element + _.withClass "gorgeous2"

    elements thatFollowRule {
      hasTag("div") &&
        hasId("toSearch") &&
        rule {
          element.classes contains ("gorgeous")
        } &&
        rule {
          element.parent.tag == "div"
        }

    }
  exports as:
    batch:
      strategy:
        results.toJson() -> File()
      aggregation:

    batch:
      results.toCSV()

    stream:
      results.map(_.toString)

*/




