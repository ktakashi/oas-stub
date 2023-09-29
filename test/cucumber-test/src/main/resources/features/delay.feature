Feature: Delaying

  @delay
  Scenario Outline: Delay API
    Given this API definition '/schema/v3/petstore.yaml'
    When I create 'petstore' API definition
    And I update API definition with '{"type": "fixed", "delay": "200"}' via '/delay' of content type 'application/json'
    And I update API '<api>' with '<delay>' via '/delay' of content type 'application/json'
    And I 'GET' to '<path>' with '' as ''
    Then I get http status 200
    And I waited at least <least> 'milliseconds'
    And I waited at most <most> 'milliseconds'

    Examples:
    @v3 @petstore
    Examples:
      | api      | delay                            | path       | least | most |
      | /v1/pets | classpath:/json/fixed-delay.json | /v1/pets   | 500   | 600  |
      | /v1/pets | classpath:/json/fixed-delay.json | /v1/pets/1 | 200   | 300  |

  @delay @custom
  Scenario Outline: Delay custom API
    Given This is custom controller tests
    When I create 'custom' API definition with '{}'
    And I update API definition with '{"type": "fixed", "delay": "200"}' via '/delay' of content type 'application/json'
    And I update API '<api>' with '<delay>' via '/delay' of content type 'application/json'
    And I 'GET' to '<path>' with '' as ''
    Then I get http status 200
    And I waited at least <least> 'milliseconds'
    And I waited at most <most> 'milliseconds'
    @v3 @petstore
    Examples:
      | api  | delay                            | path | least | most |
      | /ok1 | classpath:/json/fixed-delay.json | /ok1 | 500   | 600  |
      | /ok1 | classpath:/json/fixed-delay.json | /ok2 | 200   | 300  |
      | /ok3 | classpath:/json/fixed-delay.json | /ok3 | 500   | 600  |
      | /ok1 | classpath:/json/fixed-delay.json | /ok3 | 200   | 300  |
      | /ok4 | classpath:/json/fixed-delay.json | /ok4 | 500   | 600  |
      | /ok1 | classpath:/json/fixed-delay.json | /ok4 | 200   | 300  |

