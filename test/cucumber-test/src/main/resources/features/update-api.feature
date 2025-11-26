Feature: Update single API

  @update @plugin @single
  Scenario Outline: Update API plugin
    Given this API definition '<schema>'
    When I create '<context>' API definition
    And I update API '<api>' with '<plugin>' via '/plugins/groovy' of content type 'application/octet-stream'
    Then I get http status 200
    And I '<method>' to '<path>' with '<content>' as '<contentType>'
    Then I get http status <status>
    And I get response header of 'content-type' with '<responseContentType>'
    And I get response JSON satisfies this '<response>'
    @v3 @petstore
    Examples:
      | schema                   | context  | api              | plugin                                          | method | path       | content | contentType | status | responseContentType | response  |
      | /schema/v3/petstore.yaml | petstore | /v1/pets         | classpath:/plugins/PetStoreGetPetsPlugin.groovy | GET    | /v1/pets   |         |             | 200    | application/json    | #size()=1 |
      | /schema/v3/petstore.yaml | petstore | /v1/pets/1       | classpath:/plugins/PetStoreGetPetPlugin.groovy  | GET    | /v1/pets/1 |         |             | 200    | application/json    | id=1      |
      | /schema/v3/petstore.yaml | petstore | /v1/pets/1       | classpath:/plugins/PetStoreGetPetPlugin.groovy  | GET    | /v1/pets/2 |         |             | 200    | application/json    | id=0      |
      | /schema/v3/petstore.yaml | petstore | /v1/pets/{petId} | classpath:/plugins/PetStoreGetPetPlugin.groovy  | GET    | /v1/pets/2 |         |             | 200    | application/json    | id=1      |

  @update @plugin @single @error
  Scenario Outline: Update API plugin with non existing API
    Given this API definition '<schema>'
    When I create '<context>' API definition
    And I update API '<api>' with '<plugin>' via '/plugins/groovy' of content type 'application/octet-stream'
    Then I get http status <status>
    @v3 @petstore
    Examples:
      | schema                   | context  | api           | plugin                                          | status |
      | /schema/v3/petstore.yaml | petstore | /v1/petS      | classpath:/plugins/PetStoreGetPetsPlugin.groovy | 404    |
      # FIXME we can't check this (path variable value, 'a' is not allowed)
      | /schema/v3/petstore.yaml | petstore | /v1/pets/a    | classpath:/plugins/PetStoreGetPetPlugin.groovy  | 200    |
      # Different path variable name is allowed
      | /schema/v3/petstore.yaml | petstore | /v1/pets/{id} | classpath:/plugins/PetStoreGetPetPlugin.groovy  | 200    |

  @delete @plugin @single
  Scenario Outline: Delete API plugin
    Given this API definition '<schema>'
    When I create '<context>' API definition
    And I update API '<api>' with '<plugin>' via '/plugins/groovy' of content type 'application/octet-stream'
    Then I get http status 200
    And I delete API '<api>' via '/plugins'
    Then I get http status 204
    And I '<method>' to '<path>' with '<content>' as '<contentType>'
    Then I get http status <status>
    And I get response header of 'content-type' with '<responseContentType>'
    And I get response JSON satisfies this '<response>'
    @v3 @petstore
    Examples:
      | schema                   | context  | api              | plugin                                          | method | path       | content | contentType | status | responseContentType | response   |
      | /schema/v3/petstore.yaml | petstore | /v1/pets         | classpath:/plugins/PetStoreGetPetsPlugin.groovy | GET    | /v1/pets   |         |             | 200    | application/json    | #size()=10 |
      | /schema/v3/petstore.yaml | petstore | /v1/pets/1       | classpath:/plugins/PetStoreGetPetPlugin.groovy  | GET    | /v1/pets/1 |         |             | 200    | application/json    | id=0       |
      | /schema/v3/petstore.yaml | petstore | /v1/pets/1       | classpath:/plugins/PetStoreGetPetPlugin.groovy  | GET    | /v1/pets/2 |         |             | 200    | application/json    | id=0       |
      | /schema/v3/petstore.yaml | petstore | /v1/pets/{petId} | classpath:/plugins/PetStoreGetPetPlugin.groovy  | GET    | /v1/pets/2 |         |             | 200    | application/json    | id=0       |

  @update @options @single
  Scenario Outline: Update single API options
    Given this API definition '<schema>'
    When I create '<context>' API definition
    And I update API '<api>' with '{"shouldValidate": false}' via '/options' of content type 'application/json'
    Then I get http status 200
    Then I '<method>' to '<path>' with '<content>' as '<contentType>'
    And I get http status <status>
    @v2 @uber
    Examples:
      | schema               | context | method | api          | path                                                       | content | contentType | status |
      | /schema/v2/uber.yaml | uber    | GET    | /v1/products | /v1/products?latitude=1.0&longitude=1.5&server_token=token |         |             | 200    |
      | /schema/v2/uber.yaml | uber    | GET    | /v1/products | /v1/products?latitude=a&longitude=1.5&server_token=token   |         |             | 200    |
      | /schema/v2/uber.yaml | uber    | GET    | /v1/products | /v1/products?latitude=1.0&longitude=1.5                    |         |             | 200    |
    @v3 @petstore-extended
    Examples:
      | schema                            | context  | method | api      | path          | content        | contentType      | status |
      | /schema/v3/petstore-extended.yaml | petstore | POST   | /v2/pets | /v2/pets      | {"tag": "Cat"} | application/json | 200    |
      | /schema/v3/petstore-extended.yaml | petstore | DELETE | /v2/pets | /v2/pets/tama |                |                  | 400    |

  @update @headers @single @request
  Scenario Outline: Update single API headers request
    Given this API definition '<schema>'
    When I create '<context>' API definition
    And I 'GET' to '<path>' with '' as ''
    Then I get http status 400
    And I update API '<api>' with '<config>' via '/headers' of content type 'application/json'
    Then I get http status 200
    Then I 'GET' to '<path>' with '' as ''
    Then I get http status <status>
    @v3 @test-api
    Examples:
      | schema                | context  | api       | path        | config                                       | status |
      | /schema/test-api.yaml | test-api | /profiles | /profiles   | classpath:/json/test-api-headers-config.json | 200    |
      | /schema/test-api.yaml | test-api | /profiles | /profiles/1 | classpath:/json/test-api-headers-config.json | 400    |

  @update @headers @single @response
  Scenario Outline: Update single API headers response
    Given this API definition '<schema>'
    Given these HTTP headers
      | name       | value                                |
      | Request-ID | cca43b93-a4ff-46cf-8564-d8e4f3899657 |
    When I create '<context>' API definition
    And I update API '<api>' with '<config>' via '/headers' of content type 'application/json'
    Then I get http status 200
    Then I 'GET' to '<path>' with '' as ''
    Then I get http status 200
    And I get response header of 'X-Trace-ID' with '<response>'
    @v3 @test-api
    Examples:
      | schema                | context  | api       | path        | config                                       | response |
      | /schema/test-api.yaml | test-api | /profiles | /profiles   | classpath:/json/test-api-headers-config.json | trace-id |
      | /schema/test-api.yaml | test-api | /profiles | /profiles/1 | classpath:/json/test-api-headers-config.json | <null>   |

  @update @data @single
  Scenario Outline: Update single API data
    Given this API definition '<schema>'
    Given these HTTP headers
      | name       | value                                |
      | Request-ID | cca43b93-a4ff-46cf-8564-d8e4f3899657 |
    When I create '<context>' API definition
    And I update API '<api>' with '{"profile": {"name": "OAS API stub"}}' via '/data' of content type 'application/json'
    And I update API '<path>' with '<plugin>' via '/plugins/groovy' of content type 'application/octet-stream'
    Then I get http status 200
    Then I 'GET' to '<path>' with '' as ''
    Then I get http status 200
    And I get response JSON satisfies this '<response>'
    @v3 @test-api
    Examples:
      | schema                | context  | api         | path        | response          | plugin                                                        |
      | /schema/test-api.yaml | test-api | /profiles/1 | /profiles/1 | name=OAS API stub | classpath:/plugins/TestApiGetProfileApiDataAwarePlugin.groovy |
      | /schema/test-api.yaml | test-api | /profiles   | /profiles/1 | name=string       | classpath:/plugins/TestApiGetProfileApiDataAwarePlugin.groovy |
