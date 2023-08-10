Feature: Create API from OAS file

  Scenario Outline: Create Stub APIs
    Given this API definition '<schema>'
    When I create 'petstore' API definition
    And I '<method>' to '<path>' with '<content>' as '<contentType>'
    Then I get this <status>
    And I get response JSON satisfies this '<response>'
    @v3
    Examples:
      | schema                   | method | path     | content | contentType | status | response               |
      | /schema/v3/petstore.yaml | GET    | /v1/pets |         |             | 200    | $.size().toString()=10 |
    @v2
    Examples:
      | schema                   | method | path     | content | contentType | status | response               |
      | /schema/v2/petstore.yaml | GET    | /v1/pets |         |             | 200    | $.size().toString()=1  |
