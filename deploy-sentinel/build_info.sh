#!/bin/sh

BuildInfo=$(ls /mnt/deploy-sentinel -l | cut -d'/' -f 6)
echo $BuildInfo
