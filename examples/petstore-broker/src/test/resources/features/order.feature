Feature: Order pet

  Scenario Outline:
    When I get pets
    Then I get 200
    And I buy a pet of <id>
    Then I get <status>
    And I get order reference of '<reference>'
    Examples:
      | id | status | reference       |
      | 1  | 200    | id-1,name-Tama  |
      | 2  | 200    | id-2,name-Pochi |