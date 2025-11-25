Feature: Update single API by method

  @update @method @plugin @single
  Scenario Outline: Update API plugin by method
    Given this API definition '/schema/v3/petstore-extended.yaml'
    When I create 'petsture' API definition
    And I update API '<api>' of '<method>' method with 'classpath:/plugins/<plugin>' via '/plugins/groovy' of content type 'application/octet-stream'
    Then I get http status 200
    And I '<method>' to '<path>' with '' as ''
    Then I get http status <status>
    And I get response header of 'content-type' with '<responseContentType>'
    And I get response JSON satisfies this '<response>'
    And I '<check method>' to '<path>' with '<check content>' as '<check content type>'
    And I get http status <check status>
    @v3 @petstore
    Examples:
      | api           | plugin                       | method | path       | status | responseContentType | response  | check method | check content    | check content type | check status |
      | /v2/pets      | PetStoreGetPetsPlugin.groovy | GET    | /v2/pets   | 200    | application/json    | #size()=1 | POST         | {"name": "Tama"} | application/json   | 200          |
      | /v2/pets/{id} | PetStoreGetPetPlugin.groovy  | GET    | /v2/pets/1 | 200    | application/json    | id=1      | DELETE       |                  |                    | 204          |

  @update @method @options @single
  Scenario Outline: Update API options by method
    Given this API definition '/schema/v3/petstore-extended.yaml'
    When I create 'petsture' API definition
    And I update API '<api>' of '<method>' method with '{"shouldValidate": false}' via '/options' of content type 'application/json'
    Then I get http status 200
    And I '<method>' to '<path>' with '<content>' as 'application/json'
    Then I get http status <status>
    And I '<check method>' to '<check path>' with '' as '<check content type>'
    And I get http status <check status>
    @v3 @petstore
    Examples:
      | api           | method | path       | content        | status | check method | check path       | check content type | check status |
      | /v2/pets      | POST   | /v2/pets   | {"tag": "Cat"} | 200    | GET          | /v2/pets?limit=a | application/json   | 400          |
      | /v2/pets/{id} | GET    | /v2/pets/a |                | 200    | DELETE       | /v2/pets/a       |                    | 400          |

  @update @method @headers @single @request
  Scenario Outline: Update single API headers request by method
    Given this API definition '/schema/test-api.yaml'
    When I create 'test-api' API definition
    And I update API '<api>' of '<method>' method with 'classpath:/json/<config>' via '/headers' of content type 'application/json'
    Then I get http status 200
    When I '<method>' to '<path>' with '' as ''
    Then I get http status <status>
    When I '<check method>' to '<path>' with '<check content>' as '<check type>'
    Then I get http status <check status>
    @v3 @test-api
    Examples:
      | api       | method | path      | config                       | status | check method | check content | check type       | check status |
      | /profiles | GET    | /profiles | test-api-headers-config.json | 200    | POST         | {}            | application/json | 400          |

  @update @method @headers @single @response
  Scenario Outline: Update single API headers response by method
    Given this API definition '/schema/test-api.yaml'
    Given these HTTP headers
      | name       | value                                |
      | Request-ID | cca43b93-a4ff-46cf-8564-d8e4f3899657 |
    When I create 'test-api' API definition
    And I update API '<api>' of '<method>' method with 'classpath:/json/<config>' via '/headers' of content type 'application/json'
    Then I get http status 200
    When I '<method>' to '<path>' with '' as ''
    Then I get http status <status>
    And I get response header of 'X-Trace-ID' with '<response header>'
    When I '<check method>' to '<path>' with '<check content>' as '<check type>'
    Then I get http status <check status>
    And I get response header of 'X-Trace-ID' with '<null>'
    @v3 @test-api
    Examples:
      | api       | method | path      | config                       | status | response header | check method | check content | check type       | check status |
      | /profiles | GET    | /profiles | test-api-headers-config.json | 200    | trace-id        | POST         | {}            | application/json | 201          |
