Feature: String pattern

  @pattern
  Scenario Outline: Pattern
    Given this API definition '/schema/pattern.yaml'
    When I create 'pattern' API definition
    And I 'GET' to '<path>' with '' as ''
    Then I get pattern of '<body>' as response
    Examples:
      | path      | body                                                                          |
      | /pattern0 | "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" |
      | /pattern1 | "\d\d(\.\d+)?-\w\w\w*"                                                        |
      | /pattern2 | "[\w.%+-]+@[\w.-]+\.[a-zA-Z]{2,10}"                                           |

