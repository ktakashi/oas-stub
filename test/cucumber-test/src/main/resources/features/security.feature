Feature: Security check

  @security @http
  Scenario Outline: Security check HTTP
    Given this API definition '/schema/security-test.yaml'
    When I create 'security' API definition
    And I update API definition with '{"request": {"<header-name>": ["<header-value>"]}}' via '/headers' of content type 'application/json'
    And I 'GET' to '<path>' with '' as ''
    Then I get http status <status>
    Examples:
      | path    | status | header-name   | header-value  |
      | /basic  | 200    | Authorization | Basic blabla  |
      | /basic  | 401    |               |               |
      | /bearer | 200    | Authorization | Bearer blabla |
      | /bearer | 401    |               |               |
      | /apikey | 200    | X-API-Key     | api key       |
      | /apikey | 401    |               |               |

  # Below 2 are not supported yet. Not sure if we should actually do
  # proper token retrieval et al to check. For now, just forget about it
  @security @oauth2 @ignore
  Scenario Outline: Security check HTTP
    Given this API definition '/schema/security-test.yaml'
    When I create 'security' API definition
    And I 'GET' to '<path>' with '' as ''
    Then I get http status <status>
    Examples:
      | path    | status |
      | /oauth2 | 401    |

  @security @openid @ignore
  Scenario Outline: Security check HTTP
    Given this API definition '/schema/security-test.yaml'
    When I create 'security' API definition
    And I 'GET' to '<path>' with '' as ''
    Then I get http status <status>
    Examples:
      | path    | status |
      | /openid | 401    |
