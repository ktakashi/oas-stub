Feature: Delaying

  Scenario Outline: Delay API
    Given this API definition '/schema/v3/petstore.yaml'
    When I create 'petstore' API definition
    And I update API definition with '{"type": "fixed", "fixedDelay": "200"}' via '/delay' of content type 'application/json'
    And I update API '<api>' with '<delay>' via '/delay' of content type 'application/json'
    And I 'GET' to '<path>' with '' as ''
    Then I get http status 200
    And I waited at least <least> 'milliseconds'
    And I waited at most <most> 'milliseconds'

    Examples:
    @v3 @petstore
    Examples:
      | api      | delay                            | path       | least | most |
      | /v1/pets | classpath:/json/fixed-delay.json | /v1/pets   | 500   | 550  |
      | /v1/pets | classpath:/json/fixed-delay.json | /v1/pets/1 | 200   | 300  |
