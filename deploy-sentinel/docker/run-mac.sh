#!/bin/bash

docker run -d \
	--net=host \
	--name deploy-sentinel \
	-e "ZK_CLUSTER=$(ipconfig getifaddr en4)" \
	-p 8000:8000 \
	-v /var/serverset:/var/serverset \
	-v /mnt/log/deploy-sentinel:/var/log/deploy-sentinel deploy-sentinel
