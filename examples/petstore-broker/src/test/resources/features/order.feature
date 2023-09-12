Feature: Order pet

  Scenario Outline: Buying pets
    When I get pets
    Then I get 200
    And I buy a pet of <id>
    Then I get <status>
    And I get order reference of '<reference>'
    And I get order ID of '*random UUID in prod*'
    And 'petstore' API '/v2/pets/<id>' is called 1 time
    And 'order' API '/v1/order' is called 1 time
    Examples:
      | id | status | reference       |
      | 1  | 200    | id-1,name-Tama  |
      | 2  | 200    | id-2,name-Pochi |

  Scenario Outline: Pet not found
    Given this pet is not found <id>
    When I buy a pet of <id>
    Then I get <status>
    Examples:
      | id | status |
      | 3  | 404    |