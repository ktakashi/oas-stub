Feature: Update single API

  @update @plugin @single
  Scenario Outline: Update API plugin
    Given this API definition '<schema>'
    When I create '<context>' API definition
    And I update API '<api>' with '<plugin>' via '/plugins/groovy' of content type 'application/octet-stream'
    Then I get this 200
    And I '<method>' to '<path>' with '<content>' as '<contentType>'
    Then I get this <status>
    And I get response header of 'content-type' with '<responseContentType>'
    And I get response JSON satisfies this '<response>'
    @v3 @petstore
    Examples:
      | schema                   | context  | api              | plugin                                          | method | path       | content | contentType | status | responseContentType | response              |
      | /schema/v3/petstore.yaml | petstore | /v1/pets         | classpath:/plugins/PetStoreGetPetsPlugin.groovy | GET    | /v1/pets   |         |             | 200    | application/json    | $.size().toString()=1 |
      | /schema/v3/petstore.yaml | petstore | /v1/pets/1       | classpath:/plugins/PetStoreGetPetPlugin.groovy  | GET    | /v1/pets/1 |         |             | 200    | application/json    | id.toString()=1       |
      | /schema/v3/petstore.yaml | petstore | /v1/pets/1       | classpath:/plugins/PetStoreGetPetPlugin.groovy  | GET    | /v1/pets/2 |         |             | 200    | application/json    | id.toString()=0       |
      | /schema/v3/petstore.yaml | petstore | /v1/pets/{petId} | classpath:/plugins/PetStoreGetPetPlugin.groovy  | GET    | /v1/pets/2 |         |             | 200    | application/json    | id.toString()=1       |

  @update @plugin @single @error
  Scenario Outline: Update API plugin with non existing API
    Given this API definition '<schema>'
    When I create '<context>' API definition
    And I update API '<api>' with '<plugin>' via '/plugins/groovy' of content type 'application/octet-stream'
    Then I get this <status>
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
    Then I get this 200
    And I delete API '<api>' via '/plugins/groovy'
    Then I get this 204
    And I '<method>' to '<path>' with '<content>' as '<contentType>'
    Then I get this <status>
    And I get response header of 'content-type' with '<responseContentType>'
    And I get response JSON satisfies this '<response>'
    @v3 @petstore
    Examples:
      | schema                   | context  | api              | plugin                                          | method | path       | content | contentType | status | responseContentType | response               |
      | /schema/v3/petstore.yaml | petstore | /v1/pets         | classpath:/plugins/PetStoreGetPetsPlugin.groovy | GET    | /v1/pets   |         |             | 200    | application/json    | $.size().toString()=10 |
      | /schema/v3/petstore.yaml | petstore | /v1/pets/1       | classpath:/plugins/PetStoreGetPetPlugin.groovy  | GET    | /v1/pets/1 |         |             | 200    | application/json    | id.toString()=0        |
      | /schema/v3/petstore.yaml | petstore | /v1/pets/1       | classpath:/plugins/PetStoreGetPetPlugin.groovy  | GET    | /v1/pets/2 |         |             | 200    | application/json    | id.toString()=0        |
      | /schema/v3/petstore.yaml | petstore | /v1/pets/{petId} | classpath:/plugins/PetStoreGetPetPlugin.groovy  | GET    | /v1/pets/2 |         |             | 200    | application/json    | id.toString()=0        |
