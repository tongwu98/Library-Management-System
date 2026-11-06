Feature: Manage library books through the REST API

  Scenario: Read the API contract
    When I send "GET" to "/openapi.yaml"
    Then the published OpenAPI contract should be returned

  Scenario: List an empty library
    When I list the books
    Then the library should contain 0 books

  Scenario: Add and retrieve a book
    When I add a book titled "  Crescendo Italian Cuisine Menu  " by "  Cristian  "
    Then the book should be created as "Crescendo Italian Cuisine Menu" by "Cristian"

  Scenario: List separate copies of the same book
    Given the library contains these books:
      | title                          | author   |
      | Crescendo Italian Cuisine Menu | Cristian |
      | Crescendo Italian Cuisine Menu | Cristian |
    When I list the books
    Then the library should contain these available books:
      | title                          | author   |
      | Crescendo Italian Cuisine Menu | Cristian |
      | Crescendo Italian Cuisine Menu | Cristian |

  Scenario Outline: Search by title or author
    Given the library contains these books:
      | title                          | author   |
      | Crescendo Italian Cuisine Menu | Cristian |
      | Crescendo Dutch Cuisine Menu   | Yemoe    |
    When I search for "<query>"
    Then the search should return only "<title>"

    Examples:
      | query        | title                          |
      | ITALIAN      | Crescendo Italian Cuisine Menu |
      | cristian     | Crescendo Italian Cuisine Menu |
      | utch cuisine | Crescendo Dutch Cuisine Menu   |
      | YEMOE        | Crescendo Dutch Cuisine Menu   |

  Scenario: Blank search lists all books
    Given an available book titled "Crescendo Italian Cuisine Menu" by "Cristian"
    When I search for "   "
    Then the library should contain these available books:
      | title                          | author   |
      | Crescendo Italian Cuisine Menu | Cristian |

  Scenario: Search without a match
    Given an available book titled "Crescendo Dutch Cuisine Menu" by "Yemoe"
    When I search for "unknown"
    Then the library should contain 0 books

  Scenario: Borrow a book
    Given an available book titled "Crescendo Italian Cuisine Menu" by "Cristian"
    When I borrow the book
    Then the book should now be borrowed

  Scenario: Return a book
    Given a borrowed book titled "Crescendo French Cuisine Menu" by "Cristian"
    When I return the book
    Then the book should now be available

  Scenario Outline: A borrowed book cannot be borrowed again or removed
    Given a borrowed book titled "Crescendo Dutch Cuisine Menu" by "Yemoe"
    When I <action> the book
    Then the request should fail with status 409 and message "<message>"

    Examples:
      | action | message                          |
      | borrow | already borrowed                 |
      | remove | Borrowed books cannot be removed |

  Scenario: An available book cannot be returned
    Given an available book titled "Crescendo Japanese Cuisine Menu" by "Yemoe"
    When I return the book
    Then the request should fail with status 409 and message "not currently borrowed"

  Scenario: Remove an available book
    Given an available book titled "Crescendo Dutch Cuisine Menu" by "Yemoe"
    When I remove the book
    Then the book should no longer exist

  Scenario Outline: Reject operations on missing books
    When I send "<method>" to "<path>"
    Then the request should fail with status 404 and message "was not found"

    Examples:
      | method | path                     |
      | GET    | /api/books/999999        |
      | DELETE | /api/books/999999        |
      | POST   | /api/books/999999/borrow |
      | POST   | /api/books/999999/return |

  Scenario Outline: Reject invalid details without adding a book
    When I submit the following book:
      """
      <json>
      """
    Then the request should fail validation for "<field>"

    Examples:
      | json                                                  | field  |
      | {"title":"","author":"Cristian"}                       | title  |
      | {"title":" \t ","author":"Yemoe"}                       | title  |
      | {"title":null,"author":"Cristian"}                     | title  |
      | {"author":"Yemoe"}                                     | title  |
      | {"title":"Crescendo Italian Cuisine Menu","author":""}  | author |
      | {"title":"Crescendo Dutch Cuisine Menu","author":"   "} | author |
      | {"title":"Crescendo French Cuisine Menu","author":null} | author |
      | {"title":"Crescendo Japanese Cuisine Menu"}            | author |

  Scenario Outline: Reject malformed or missing JSON
    When I submit the following book:
      """
      <json>
      """
    Then the request should fail with status 400

    Examples:
      | json      |
      | not-json  |
      | null      |
      | []        |
      | {"title": |
      |           |

  Scenario: Reject a nonnumeric book ID
    When I send "GET" to "/api/books/not-a-number"
    Then the request should fail with status 400
