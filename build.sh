#!/bin/sh
set -e

mkdir -p build && javac -d build tee/Tee.java tee/TeeTests.java
