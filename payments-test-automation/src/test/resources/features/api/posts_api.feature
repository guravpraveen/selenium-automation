@api
Feature: REST API - CRUD operations
  Practice API (jsonplaceholder) used to demonstrate every HTTP verb.
  Swap the paths for your payment hub endpoints, e.g. POST /payments, GET /payments/{uetr}/status.

  @smoke
  Scenario: Retrieve a single resource and validate its schema
    When I send a GET request to "/posts/1"
    Then the response status code should be 200
    And the response field "id" should be "1"
    And the response should match schema "schemas/post-schema.json"
    And the response time should be below 10000 ms

  Scenario: Filter resources with a query parameter
    Given the query parameter "userId" is "2"
    When I send a GET request to "/posts"
    Then the response status code should be 200
    And the response list "$" should have 10 items

  Scenario: Create a resource with POST
    Given the request header "X-Correlation-Id" is "e6f1a2b3-1c2d-4e5f-8a9b-0c1d2e3f4a5b"
    And the request body is:
      | userId | 9                           |
      | title  | Intraday liquidity transfer |
      | body   | INR RTGS top-up             |
    When I send a POST request to "/posts"
    Then the response status code should be 201
    And the response should contain fields:
      | title  | Intraday liquidity transfer |
      | userId | 9                           |

  Scenario: Replace a resource with PUT
    Given the request JSON is:
      """
      {"id": 1, "userId": 1, "title": "Cut-off extended", "body": "USD cut-off moved to 17:30"}
      """
    When I send a PUT request to "/posts/1"
    Then the response status code should be 200
    And the response field "title" should be "Cut-off extended"

  Scenario: Partially update a resource with PATCH
    Given the request JSON is:
      """
      {"title": "Patched"}
      """
    When I send a PATCH request to "/posts/1"
    Then the response status code should be 200
    And the response field "title" should be "Patched"

  Scenario: Delete a resource
    When I send a DELETE request to "/posts/1"
    Then the response status code should be 200

  @negative
  Scenario: Unknown resource returns 404
    When I send a GET request to "/posts/987654"
    Then the response status code should be 404
