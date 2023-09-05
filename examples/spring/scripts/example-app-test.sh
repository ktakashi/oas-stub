#!/bin/bash

ME=$(realpath $0)
DIR=$(dirname $ME)

TARGET=$DIR/../target
SRC=$DIR/../src
DIST_FILE=$TARGET/oas-stub-example-dist.jar
SCHEMA_DIR=$SRC/test/resources/schema
PLUGIN_DIR=$SRC/test/resources/plugins

## start application
java -jar $DIST_FILE &
# Wait until the application is up and running
while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' http://localhost:8080/actuator)" != "200" ]]; do sleep 5; done

# TODO add validation
# Simple check
curl -s -X POST http://localhost:8080/__admin/petstore -H'Content-Type: application/octet-stream' --data-binary '@'$SCHEMA_DIR/petstore.yaml | jq
curl -s http://localhost:8080/oas/petstore/v1/pets | jq

curl -s -X PUT -H'Content-Type: application/octet-stream' http://localhost:8080/__admin/petstore/configurations/plugins/groovy?api=/v1/pets/1 \
  --data-binary '@'$PLUGIN_DIR/petstore/PetstoreGetPetPlugin.groovy | jq
curl -s http://localhost:8080/oas/petstore/v1/pets/1 | jq
curl -s http://localhost:8080/oas/petstore/v1/pets/2 | jq

curl -X POST http://localhost:8080/actuator/shutdown
