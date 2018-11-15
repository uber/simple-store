# The following dependencies were calculated from:
#
# generate_workspace --artifact=com.google.code.findbugs:jsr305:3.0.2 --artifact=com.squareup.leakcanary:leakcanary-android:1.6.2 --repositories=https://jcenter.bintray.com


def generated_maven_jars():
  native.maven_jar(
      name = "com_google_code_findbugs_jsr305",
      artifact = "com.google.code.findbugs:jsr305:3.0.2",
      repository = "https://jcenter.bintray.com/",
      sha1 = "25ea2e8b0c338a877313bd4672d3fe056ea78f0d",
  )

def generated_java_libraries():
  native.java_library(
      name = "com_google_code_findbugs_jsr305",
      visibility = ["//visibility:public"],
      exports = ["@com_google_code_findbugs_jsr305//jar"],
  )



