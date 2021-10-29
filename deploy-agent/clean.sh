#!/bin/bash

# run this command, replacing -n with -f to actually clean up the dir
# this will clean all __pycache__, .pyc, and any other untracked files.
# this is needed to avoid mounting a dirty env into docker, until Dockerfile is improved
git clean -d -x -n
