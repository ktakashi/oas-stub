Feature: Update single API

  @update @plugin @single
  Scenario Outline: Update API plugin
    Given this API definition '<schema>'
    When I create '<context>' API definition
    And I update API '<api>' with '<plugin>' via '/plugins/groovy' of content type 'application/octet-stream'
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
