#!/bin/bash

PROJECT_TO_BUILD=$1
IMAGE_NAME_SPLIT=(${IMAGE//:/ })
IMAGE_VERSION="${IMAGE_NAME_SPLIT[1]}"

sbt "set version in ThisBuild := \"${IMAGE_VERSION}\"" "${PROJECT_TO_BUILD} / docker:publishLocal" 
#sbt --client "set version in ThisBuild := \"${IMAGE_VERSION}\"" 
#sbt --client  "${PROJECT_TO_BUILD} / compile"
#sbt --client  "${PROJECT_TO_BUILD} / docker:publishLocal"
