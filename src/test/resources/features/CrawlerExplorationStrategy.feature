Feature: Crawler exploration strategy

  The crawler exploration strategy is the ability of the crawler
  to explore, with a set of prefixed rules, the structure of a website
  where an user need to extrapolate the information.

  Is also possible to define it the "frontier" exploration which
  is relative of the new links that should be explored.

  During its creation a Crawler will take a function that describe the
  exploration strategy.

  Rule: A crawler should only one exploration strategy.

    Example: Exploration with BFS (Breadth First Search)
      Given a crawler with a Breadth First Search strategy
      When  it starts crawling the website
      Then  the nodes are explored with the breadth first search approach

  Rule: Before it starts we can assign a new exploration strategy to the crawler

    Example: Change the exploration strategy
      Given a crawler that hasn't be started yet
      When we assign a new exploration strategy
      And it starts crawling a website
      Then it will start crawl using the last exploration strategy setted

  Rule: An exploration strategy should be able to respect external constraints

    Example: Robot.txt
      Given a crawler with a certain exploration strategy
      And a website with a robot.txt file on the root path
      When it starts crawling the website
      Then it will visit only the pages listed on robot.txt
  
  Rule: A crawler can explore an external domain from the one specified in the seed URL if enabled to do that
    
    Example: Blocking external domains
      Given a crawler set up for start exploring the seed url http://www.adomain.com/
      And the crawler is not allowed to explore external domains
      When it starts crawling the website reaching http://www.bdomain.com/
      Then it will explore only the pages of the internal domain
      
    Example: Exploring http://www.bdomain.com/
      Given a crawler set up for start exploring the seed url http://www.adomain.com/
      And the crawler is allowed to explore external domains
      And the user haven't provided a white or black list
      When it starts crawling the website reaching http://www.bdomain.com/
      Then it will explore the external url

    Example: Whitelisting a website
      Given a crawler set up for start exploring the seed url http://www.adomain.com/
      And the crawler is allowed to explore external domains
      And the user provided a whitelist
      When it starts crawling the website
      Then it will visit only the pages listed on the whitelist

    Example: Blacklisting a website
      Given a crawler set up for start exploring the seed url http://www.adomain.com/
      And the user provided a blacklist
      When it starts crawling the website
      Then it will visit only the pages not listed on the blacklist

    Scenario: Provide both whitelist and blacklist
      Given a crawler setup for explore a website
      And parametrized with a white and a black list
      When it starts crawling the website
      Then it will ignore the blacklist
      
  Scenario: Custom exploration strategy

    Given an user Giovanni that want to set up a custom exploration rule for a website
    When he creates a new crawler
    Then it will start explore the website using the custom rule of Giovanni

  Scenario: Combine multiple exploration strategies

    Given an user Matteo that want create a custom exploration rule
    And a BFS exploration rule
    And a link-weighted exploration rule
    Then it will be able to combine the two rules applying first the bfs one and then the link-weighted one.



