#!/usr/bin/env bash

docker build -t api-test-container .

docker run --rm --net=host \
    -v $(pwd):/opt \
    -v /var/lib/normandie:/var/lib/normandie:ro,rslave \
    -v /usr/bin/knox:/usr/bin/knox \
    -v /var/lib/knox:/var/lib/knox \
    api-test-container \
    ./api-test.sh
