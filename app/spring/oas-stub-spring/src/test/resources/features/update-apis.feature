Feature: Update APIs

  @update @options
  Scenario Outline: Update API options
    Given this API definition '<schema>'
    When I create '<context>' API definition
    And I update API definition with '{"shouldValidate": false}' via '/options' of content type 'application/json'
    Then I get http status 200
    And I get API definition via '/options'
    Then I get http status 200
    And I get response JSON satisfies this 'shouldValidate=false'
    Then I '<method>' to '<path>' with '<content>' as '<contentType>'
    And I get http status <status>
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

  @update @headers
  Scenario Outline: Update API headers
    Given this API definition '<schema>'
    When I create '<context>' API definition
    And I 'GET' to '<path>' with '' as ''
    Then I get http status 400
    And I update API definition with '<config>' via '/headers' of content type 'application/json'
    Then I get http status 200
    And I get API definition via '/headers'
    Then I get http status 200
    And I get response JSON satisfies this 'request.Request-ID[0]=<uuid>;response.X-Trace-ID[0]=trace-id'
    Then I 'GET' to '<path>' with '' as ''
    Then I get http status 200
    And I get response header of 'X-Trace-ID' with 'trace-id'
    @v3 @test-api
    Examples:
      | schema                | context  | path      | config                                       |
      | /schema/test-api.yaml | test-api | /profiles | classpath:/json/test-api-headers-config.json |

  @update @data
  Scenario Outline: Update API data
    Given this API definition '<schema>'
    Given these HTTP headers
      | name       | value                                |
      | Request-ID | cca43b93-a4ff-46cf-8564-d8e4f3899657 |
    When I create '<context>' API definition
    And I update API definition with '<config>' via '/data' of content type 'application/json'
    And I get API definition via '/data'
    Then I get http status 200
    And I update API '<path>' with '<plugin>' via '/plugins/groovy' of content type 'application/octet-stream'
    Then I get http status 200
    Then I 'GET' to '<path>' with '' as ''
    Then I get http status 200
    And I get response JSON satisfies this '<response>'
    @v3 @test-api
    Examples:
      | schema                | context  | path        | config                                   | response            | plugin                                                        |
      | /schema/test-api.yaml | test-api | /profiles/1 | {"profile": {"name": "OAS API stub"}}    | name=OAS API stub   | classpath:/plugins/TestApiGetProfileApiDataAwarePlugin.groovy |
      | /schema/test-api.yaml | test-api | /profiles/1 | {"profile2": {"then": "OAS API stub 2"}} | then=OAS API stub 2 | classpath:/plugins/TestApiGetProfileApiDataAwarePlugin.groovy |


  @update @monitor
  Scenario Outline: Update API monitor options
    Given this API definition '<schema>'
    When I create '<context>' API definition
    And I update API definition with '{"shouldMonitor": false}' via '/options' of content type 'application/json'
    Then I get http status 200
    And I get API definition via '/options'
    Then I get http status 200
    And I get response JSON satisfies this 'shouldMonitor=false'
    Then I '<method>' to '<path>' with '<content>' as '<contentType>'
    And I get http status <status>
    Then I get metrics of '<context>'
    And I get http status 404
    @v2 @uber
    Examples:
      | schema               | context | method | path                                                       | content | contentType | status |
      | /schema/v2/uber.yaml | uber    | GET    | /v1/products?latitude=1.0&longitude=1.5&server_token=token |         |             | 200    |
    @v3 @petstore-extended
    Examples:
      | schema                            | context  | method | path       | content                        | contentType      | status |
      | /schema/v3/petstore-extended.yaml | petstore | POST   | /v2/pets   | {"name": "Tama", "tag": "Cat"} | application/json | 200    |
      | /schema/v3/petstore-extended.yaml | petstore | DELETE | /v2/pets/1 |                                |                  | 204    |
