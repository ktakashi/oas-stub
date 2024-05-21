Feature: Custom route HTTP status
  @custom @status
  Scenario Outline:
    Given This is custom controller tests
    When I create 'custom' API definition with '{}'
    And I 'PUT' to '/status' with '{"status":<status>}' as 'application/json'
    Then I get http status <status>
    Examples:
    | status |
    | 200    |
    | 201    |
    | 202    |
    | 204    |
    | 301    |
    | 302    |
    | 303    |
    | 400    |
    | 401    |
    | 403    |
    | 404    |
    | 405    |
    | 500    |
    | 502    |

