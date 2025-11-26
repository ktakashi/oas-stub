Feature: Static Stub configuration

  @static
  Scenario Outline: Create Stub APIs
    Given this static stub 'petstore-static'
    When I '<method>' to '<path>' with '<content>' as '<contentType>'
    Then I get http status <status>
    And I get response header of 'content-type' with '<responseContentType>'
    And I get response JSON satisfies this '<response>'
    Then I get API definition via ''
    Examples:
      | method | path             | content | contentType | status | responseContentType | response   |
      | GET    | /v1/pets         |         |             | 200    | application/json    | #size()=10 |
      | GET    | /v1/pets/1       |         |             | 200    | application/json    | id=1      |
      | GET    | /v1/pets/2       |         |             | 200    | application/json    | id=1      |
      | POST   | /v1/pets         |         |             | 201    | <null>              | <null>     |
      | GET    | /v1/pets?limit=1 |         |             | 200    | application/json    | #size()=10 |
      | GET    | /v1/pets?limit=a |         |             | 400    | application/json    | code=0    |