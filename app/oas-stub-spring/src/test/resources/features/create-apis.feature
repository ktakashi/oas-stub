Feature: Create APIs from OAS file

  @creation
  Scenario Outline: Create Stub APIs
    Given this API definition '<schema>'
    When I create 'petstore' API definition
    And I '<method>' to '<path>' with '<content>' as '<contentType>'
    Then I get this <status>
    And I get response header of 'content-type' with '<responseContentType>'
    And I get response JSON satisfies this '<response>'
    @v3 @petstore
    Examples:
      | schema                   | method | path             | content | contentType | status | responseContentType | response               |
      | /schema/v3/petstore.yaml | GET    | /v1/pets         |         |             | 200    | application/json    | $.size().toString()=10 |
      | /schema/v3/petstore.yaml | GET    | /v1/pets/1       |         |             | 200    | application/json    | id.toString()=0        |
      | /schema/v3/petstore.yaml | POST   | /v1/pets         |         |             | 201    | <null>              | <null>                 |
      | /schema/v3/petstore.yaml | GET    | /v1/pets?limit=1 |         |             | 200    | application/json    | $.size().toString()=10 |
      | /schema/v3/petstore.yaml | GET    | /v1/pets?limit=a |         |             | 400    | application/json    | code.toString()=0      |
    @v3 @petstore-extended
    Examples:
      | schema                            | method | path          | content                        | contentType      | status | responseContentType      | response          |
      | /schema/v3/petstore-extended.yaml | POST   | /v2/pets      | {"name": "Tama", "tag": "Cat"} | application/json | 200    | application/json         | name=string       |
      | /schema/v3/petstore-extended.yaml | POST   | /v2/pets      | {"tag": "Cat"}                 | application/json | 400    | application/json         | message=string    |
      | /schema/v3/petstore-extended.yaml | POST   | /v2/pets      | {"name": "Pochi", "tag": 1}    | application/json | 400    | application/json         | message=string    |
      | /schema/v3/petstore-extended.yaml | DELETE | /v2/pets/1    |                                |                  | 204    | <null>                   | <null>            |
      | /schema/v3/petstore-extended.yaml | DELETE | /v2/pets/tama |                                |                  | 400    | application/problem+json | errors[0].name=id |
    @v2 @petstore
    Examples:
      | schema                   | method | path       | content | contentType | status | responseContentType | response              |
      | /schema/v2/petstore.yaml | GET    | /v1/pets   |         |             | 200    | application/json    | $.size().toString()=1 |
      | /schema/v2/petstore.yaml | GET    | /v1/pets/2 |         |             | 200    | application/json    | [0].id.toString()=0   |
      | /schema/v2/petstore.yaml | POST   | /v1/pets   |         |             | 201    | <null>              | <null>                |
    @v2 @uber
    Examples:
      | schema               | method | path                                                       | content | contentType | status | responseContentType | response              |
      | /schema/v2/uber.yaml | GET    | /v1/products?latitude=1.0&longitude=1.5&server_token=token |         |             | 200    | application/json    | [0].product_id=string |
      | /schema/v2/uber.yaml | GET    | /v1/products?latitude=1.0&longitude=1.5                    |         |             | 401    | application/json    | message=string        |
      | /schema/v2/uber.yaml | GET    | /v1/me                                                     |         |             | 200    | application/json    | first_name=string     |
