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
      | schema                   | context  | api      | plugin                                        | method | path     | content | contentType | status | responseContentType | response   |
      | /schema/v3/petstore.yaml | petstore | /v1/pets | classpath:/plugins/PetStoreErrorPlugin.groovy | GET    | /v1/pets |         |             | 200    | application/json    | #size()=10 |

  @error @failure @protocol
  Scenario Outline: Protocol error
    Given this API definition '/schema/v3/petstore.yaml'
    When I create 'petstore' API definition
    And I update API definition with '<failure>' via '/options' of content type 'application/json'
    Then I get http status 200
    Then [Protocol Error] I 'GET' to '/v1/pets' with '' as ''
    Examples:
      | failure                           |
      | {"failure": {"type": "protocol"}} |

  @error @failure @http
  Scenario Outline: Http error
    Given this API definition '/schema/v3/petstore.yaml'
    When I create 'petstore' API definition
    And I update API definition with '<failure>' via '/options' of content type 'application/json'
    Then I get http status 200
    Then I 'GET' to '/v1/pets' with '' as ''
    And I get http status <status>
    Examples:
      | failure                                      | status |
      | {"failure": {"type": "http"}}                | 500    |
      | {"failure": {"type": "http", "status": 503}} | 503    |

  @error @failure @latency
  Scenario Outline: High latency
    Given this API definition '/schema/test-api.yaml'
    Given these HTTP headers
      | name       | value                                |
      | Request-ID | cca43b93-a4ff-46cf-8564-d8e4f3899657 |
    When I create 'test-api' API definition
    And I update API definition with '<failure>' via '/options' of content type 'application/json'
    Then I get http status 200
    Then I 'GET' to '/profiles/1' with '' as ''
    And I get http status <status>
    And Reading response took at least <least> 'milliseconds'
    Examples:
      | failure                                                | status | least |
      | {"latency": {"interval": 100, "unit": "MILLISECONDS"}} | 200    | 1500  |

  @error @failure @connection
  Scenario Outline: Connection error
    Given this API definition '/schema/test-api.yaml'
    Given these HTTP headers
      | name       | value                                |
      | Request-ID | cca43b93-a4ff-46cf-8564-d8e4f3899657 |
    When I create 'test-api' API definition
    And I update API definition with '<failure>' via '/options' of content type 'application/json'
    Then I get http status 200
    Then [Connection Error] I 'GET' to '/profiles/1' with '' as ''
    Examples:
      | failure                             |
      | {"failure": {"type": "connection"}} |

  @error @custom @failure @protocol
  Scenario Outline: Protocol error
    Given This is custom controller tests
    When I create 'custom' API definition with '{}'
    And I update API definition with '<failure>' via '/options' of content type 'application/json'
    Then I get http status 200
    Then [Protocol Error] I 'GET' to '/ok1' with '' as ''
    Examples:
      | failure                           |
      | {"failure": {"type": "protocol"}} |

  @error @custom @failure @http
  Scenario Outline: Http error
    Given This is custom controller tests
    When I create 'custom' API definition with '{}'
    And I update API definition with '<failure>' via '/options' of content type 'application/json'
    Then I get http status 200
    Then I 'GET' to '/ok1' with '' as ''
    And I get http status <status>
    Examples:
      | failure                                      | status |
      | {"failure": {"type": "http"}}                | 500    |
      | {"failure": {"type": "http", "status": 503}} | 503    |

    # Ignore for now, need to change the way handling response content
  @error @custom @failure @latency @ignore
  Scenario Outline: High latency
    Given This is custom controller tests
    When I create 'custom' API definition with '{}'
    And I update API definition with '<failure>' via '/options' of content type 'application/json'
    Then I get http status 200
    Then I 'GET' to '/ok1' with '' as ''
    And I get http status <status>
    And Reading response took at least <least> 'milliseconds'
    Examples:
      | failure                                                | status | least |
      | {"latency": {"interval": 100, "unit": "MILLISECONDS"}} | 200    | 1500  |

  @error @custom @failure @connection
  Scenario Outline: Connection error
    Given This is custom controller tests
    When I create 'custom' API definition with '{}'
    And I update API definition with '<failure>' via '/options' of content type 'application/json'
    Then I get http status 200
    Then [Connection Error] I 'GET' to '/ok1' with '' as ''
    Examples:
      | failure                             |
      | {"failure": {"type": "connection"}} |
