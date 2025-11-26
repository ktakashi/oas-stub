Feature: Create APIs from OAS file

  @creation
  Scenario Outline: Create Stub APIs
    Given these HTTP headers
      | name       | value                                |
      | Request-ID | cca43b93-a4ff-46cf-8564-d8e4f3899657 |
    Given this API definition '<schema>'
    When I create 'petstore' API definition
    And I '<method>' to '<path>' with '<content>' as '<contentType>'
    Then I get http status <status>
    And I get response header of 'content-type' with '<responseContentType>'
    And I get response JSON satisfies this '<response>'
    Then I get API definition via ''
    And I get http status 200
    And I get response JSON satisfies this 'configurations=<null>;headers=<null>;options.shouldMonitor=true;data=<null>;delay=<null>'
    And I get API definition via '/data'
    And I get http status 404
    Then I get API definition via '/options'
    And I get http status 200
    Then I get API definition via '/configurations'
    And I get http status 404
    Then I get API definition via '/headers'
    And I get http status 404
    Then I get API definition via '/delay'
    And I get http status 404
    Then I get metrics of 'petstore'
    And I get http status 200
    Then I delete all metrics
    And I get http status 204
    @v3 @petstore
    Examples:
      | schema                   | method | path             | content | contentType | status | responseContentType | response   |
      | /schema/v3/petstore.yaml | GET    | /v1/pets         |         |             | 200    | application/json    | #size()=10 |
      | /schema/v3/petstore.yaml | GET    | /v1/pets/1       |         |             | 200    | application/json    | id=0       |
      | /schema/v3/petstore.yaml | POST   | /v1/pets         |         |             | 201    | <null>              | <null>     |
      | /schema/v3/petstore.yaml | GET    | /v1/pets?limit=1 |         |             | 200    | application/json    | #size()=10 |
      | /schema/v3/petstore.yaml | GET    | /v1/pets?limit=a |         |             | 400    | application/json    | code=0     |
    @v3 @petstore-extended
    Examples:
      | schema                            | method | path          | content                        | contentType      | status | responseContentType | response       |
      | /schema/v3/petstore-extended.yaml | POST   | /v2/pets      | {"name": "Tama", "tag": "Cat"} | application/json | 200    | application/json    | name=string    |
      | /schema/v3/petstore-extended.yaml | POST   | /v2/pets      | {"tag": "Cat"}                 | application/json | 400    | application/json    | message=string |
      | /schema/v3/petstore-extended.yaml | POST   | /v2/pets      | {"name": "Pochi", "tag": 1}    | application/json | 400    | application/json    | message=string |
      | /schema/v3/petstore-extended.yaml | DELETE | /v2/pets/1    |                                |                  | 204    | <null>              | <null>         |
      | /schema/v3/petstore-extended.yaml | DELETE | /v2/pets/tama |                                |                  | 400    | application/json    | message=string |
    @v3_1 @petstore
    Examples:
      | schema                     | method | path             | content | contentType | status | responseContentType | response   |
      | /schema/v3_1/petstore.yaml | GET    | /v1/pets         |         |             | 200    | application/json    | #size()=10 |
      | /schema/v3_1/petstore.yaml | GET    | /v1/pets/1       |         |             | 200    | application/json    | id=0       |
      | /schema/v3_1/petstore.yaml | POST   | /v1/pets         |         |             | 201    | <null>              | <null>     |
      | /schema/v3_1/petstore.yaml | GET    | /v1/pets?limit=1 |         |             | 200    | application/json    | #size()=10 |
      | /schema/v3_1/petstore.yaml | GET    | /v1/pets?limit=a |         |             | 400    | application/json    | code=0     |
    @v2 @petstore
    Examples:
      | schema                   | method | path       | content | contentType | status | responseContentType | response  |
      | /schema/v2/petstore.yaml | GET    | /v1/pets   |         |             | 200    | application/json    | #size()=1 |
      | /schema/v2/petstore.yaml | GET    | /v1/pets/2 |         |             | 200    | application/json    | [0]/id=0  |
      | /schema/v2/petstore.yaml | POST   | /v1/pets   |         |             | 201    | <null>              | <null>    |
    @v2 @uber
    Examples:
      | schema               | method | path                                                       | content | contentType | status | responseContentType | response              |
      | /schema/v2/uber.yaml | GET    | /v1/products?latitude=1.0&longitude=1.5&server_token=token |         |             | 200    | application/json    | [0].product_id=string |
      | /schema/v2/uber.yaml | GET    | /v1/products?latitude=1.0&longitude=1.5                    |         |             | 401    | application/json    | message=string        |
      | /schema/v2/uber.yaml | GET    | /v1/me                                                     |         |             | 200    | application/json    | first_name=string     |
    @v3_1 @test-api
    Examples:
      | schema                | method | path              | content          | contentType      | status | responseContentType | response   |
      | /schema/test-api.yaml | GET    | /examples         |                  |                  | 200    | application/json    | attr=email |
      | /schema/test-api.yaml | GET    | /examples/objects |                  |                  | 200    | application/json    | attr=uuid  |
      | /schema/test-api.yaml | POST   | /profiles         | {"name": "name"} | application/json | 201    | <null>              | <null>     |

  @creation @records
  Scenario Outline: Create Stub APIs with records
    Given these HTTP headers
      | name       | value                                |
      | Request-ID | cca43b93-a4ff-46cf-8564-d8e4f3899657 |
    Given this API definition '<schema>'
    When I create 'petstore' API definition
    And I update API definition with '{"shouldRecord": true}' via '/options' of content type 'application/json'
    And I '<method>' to '<path>' with '' as ''
    Then I get http status <status>
    Then I get records of 'petstore'
    And I get http status 200
    @v3 @petstore
    Examples:
      | schema                   | method | path       | status |
      | /schema/v3/petstore.yaml | GET    | /v1/pets   | 200    |
      | /schema/v3/petstore.yaml | GET    | /v1/pets/1 | 200    |

  @deletion
  Scenario Outline: Delete Stub APIs
    Given this API definition '<schema>'
    When I create 'petstore' API definition
    Then I delete the API definition
    And I '<method>' to '<path>' with '' as ''
    Then I get http status 404
    Then I get API definition via ''
    And I get http status 404
    @v3 @petstore
    Examples:
      | schema                   | method | path     |
      | /schema/v3/petstore.yaml | GET    | /v1/pets |
