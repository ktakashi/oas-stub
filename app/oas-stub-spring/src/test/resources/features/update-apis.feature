Feature: Update APIs

  @update @options
  Scenario Outline: Update API options
    Given this API definition '<schema>'
    When I create '<context>' API definition
    And I update API definition with '{"shouldValidate": false}' via '/<context>/options' of content type 'application/json'
    Then I get this 200
    Then I '<method>' to '<path>' with '<content>' as '<contentType>'
    And I get this <status>
    @v2 @uber
    Examples:
      | schema               | context | method | path                                                       | content | contentType | status |
      | /schema/v2/uber.yaml | uber    | GET    | /v1/products?latitude=1.0&longitude=1.5&server_token=token |         |             | 200    |
      | /schema/v2/uber.yaml | uber    | GET    | /v1/products?latitude=a&longitude=1.5&server_token=token   |         |             | 200    |
      | /schema/v2/uber.yaml | uber    | GET    | /v1/products?latitude=1.0&longitude=1.5                    |         |             | 200    |
    @v3 @petstore-extended
    Examples:
      | schema                            | context  | method | path          | content        | contentType      | status |
      | /schema/v3/petstore-extended.yaml | petstore | POST   | /v2/pets      | {"tag": "Cat"} | application/json | 200    |
      | /schema/v3/petstore-extended.yaml | petstore | DELETE | /v2/pets/tama |                |                  | 204    |
