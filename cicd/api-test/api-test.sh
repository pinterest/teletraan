#!/usr/bin/env bash

# configs for dev server
TOKEN=$(KNOX_MACHINE_AUTH=$(hostname) knox get teletraan_service_dev:api_test_token)
SERVER=https://deploy-dev.pinadmin.com
newman run --env-var token=$TOKEN --env-var baseUrl=$SERVER \
    deploy-service-api-test.postman_collection.json