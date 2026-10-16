#!/usr/bin/env bash -i -e

# This one-liner simply passes the command line arguments and binds the workdir as a mount point to it

docker run --rm --name apktool --platform=linux/amd64 --volume $(pwd -P):/data:rw -it apktool:latest "$@"
