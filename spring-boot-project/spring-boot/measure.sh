#!/bin/bash
for run in {1..5}; do
  ../../gradlew skippyAnalyze --no-build-cache --rerun-tasks --no-daemon
done
