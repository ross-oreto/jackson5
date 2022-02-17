#!/bin/sh
mvn versions:set -DnewVersion="$1" && mvn clean deploy -P release