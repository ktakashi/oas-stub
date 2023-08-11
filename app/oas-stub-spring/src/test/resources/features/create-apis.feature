Feature: Create API from OAS file

  Scenario Outline: Create Stub APIs
    Given this API definition '<schema>'
    When I create 'petstore' API definition
    And I '<method>' to '<path>' with '<content>' as '<contentType>'
    Then I get this <status>
    And I get response header of 'content-type' with '<responseContentType>'
    And I get response JSON satisfies this '<response>'
    @v3
    Examples:
      | schema                   | method | path             | content | contentType | status | responseContentType      | response               |
      | /schema/v3/petstore.yaml | GET    | /v1/pets         |         |             | 200    | application/json         | $.size().toString()=10 |
      | /schema/v3/petstore.yaml | GET    | /v1/pets/1       |         |             | 200    | application/json         | id.toString()=1        |
      | /schema/v3/petstore.yaml | POST   | /v1/pets         |         |             | 201    | <null>                   | <null>                 |
      | /schema/v3/petstore.yaml | GET    | /v1/pets?limit=1 |         |             | 200    | application/json         | $.size().toString()=10 |
      | /schema/v3/petstore.yaml | GET    | /v1/pets?limit=a |         |             | 400    | application/problem+json | errors[0].name=limit   |
    @v2
    Examples:
      | schema                   | method | path       | content | contentType | status | responseContentType | response              |
      | /schema/v2/petstore.yaml | GET    | /v1/pets   |         |             | 200    | application/json    | $.size().toString()=1 |
      | /schema/v2/petstore.yaml | GET    | /v1/pets/2 |         |             | 200    | application/json    | [0].id.toString()=1   |
      | /schema/v2/petstore.yaml | POST   | /v1/pets   |         |             | 201    | <null>              | <null>                |
