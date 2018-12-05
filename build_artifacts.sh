#!/bin/bash
bazel build //java/com/uber/simplestore/proto:bin_deploy.jar
cp bazel-bin/java/com/uber/simplestore/proto/bin_deploy.jar simplestore-proto.jar
chmod u+rw simplestore-proto.jar
zip -d simplestore-proto.jar "com/google/*"
zip -d simplestore-proto.jar "javax/*"
zip -d simplestore-proto.jar "META-INF/maven/*"
