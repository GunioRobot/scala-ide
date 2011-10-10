#!/bin/sh

VERSION_TAG=scalagwt

. $(dirname $0)/env.sh

SCALA_VERSION=2.10.0-scalagwt-SNAPSHOT
SBT_VERSION=2.10.0-SNAPSHOT
PROFILE_NAME="-P local-scala-trunk,!scala-trunk"

build $*
