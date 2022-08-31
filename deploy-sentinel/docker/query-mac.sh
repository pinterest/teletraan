#!/bin/bash

curl http://$(docker-machine ip):8000/
curl http://$(docker-machine ip):8000/health
