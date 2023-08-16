Feature: Error cases

  @error @plugin
  Scenario Outline: Plugin execution error
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
      | schema                   | context  | api      | plugin                                        | method | path     | content | contentType | status | responseContentType | response               |
      | /schema/v3/petstore.yaml | petstore | /v1/pets | classpath:/plugins/PetStoreErrorPlugin.groovy | GET    | /v1/pets |         |             | 200    | application/json    | $.size().toString()=10 |
