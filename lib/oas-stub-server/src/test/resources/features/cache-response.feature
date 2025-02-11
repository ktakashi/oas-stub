Feature: Cached response of deeply nested response
  @pain @ignore
  Scenario: PAIN.002
    Given this API definition '/pain.yaml'
    When I create 'pain' API definition
    And I 'GET' to '/pain.002.001.11' with '' as '' for 1000 times in 10 batches
    Then I get http status 200
