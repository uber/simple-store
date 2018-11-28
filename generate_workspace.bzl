# The following dependencies were calculated from:
#
# generate_workspace --artifact=com.google.code.findbugs:jsr305:3.0.2 --artifact=com.google.protobuf:protobuf-lite:3.0.1 --repositories=https://jcenter.bintray.com


def generated_maven_jars():
  native.maven_jar(
      name = "com_google_code_findbugs_jsr305",
      artifact = "com.google.code.findbugs:jsr305:3.0.2",
      repository = "https://jcenter.bintray.com/",
      sha1 = "25ea2e8b0c338a877313bd4672d3fe056ea78f0d",
  )


  native.maven_jar(
      name = "com_google_protobuf_protobuf_lite",
      artifact = "com.google.protobuf:protobuf-lite:3.0.1",
      repository = "https://jcenter.bintray.com/",
      sha1 = "59b5b9c6e1a3054696d23492f888c1f8b583f5fc",
  )




def generated_java_libraries():
  native.java_library(
      name = "com_google_code_findbugs_jsr305",
      visibility = ["//visibility:public"],
      exports = ["@com_google_code_findbugs_jsr305//jar"],
  )


  native.java_library(
      name = "com_google_protobuf_protobuf_lite",
      visibility = ["//visibility:public"],
      exports = ["@com_google_protobuf_protobuf_lite//jar"],
  )


