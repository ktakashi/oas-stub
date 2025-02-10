Feature: Value is cached but some are freshly generated

  @cached
  Scenario Outline: format uuid and datetime are generated each time
    Given these HTTP headers
      | name       | value                                |
      | Request-ID | cca43b93-a4ff-46cf-8564-d8e4f3899657 |
    Given this API definition '/schema/<api>'
    When I create 'api' API definition
    And I 'GET' to '<path>' with '' as ''
    Then I get http status 200
    And I save the response

    When I 'GET' to '<path>' with '' as ''
    Then I get http status 200
    And the responses are the same: <same?>
    Examples:
      | api              | path        | same? |
      | test-api.yaml    | /profiles/1 | no    |
      | v2/petstore.yaml | /v1/pets/1  | yes   |
