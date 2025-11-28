#!/bin/bash
npm run ncu
./gradlew --refresh-dependencies dependencyUpdates -Drevision=release --no-parallel
./gradlew --stop
