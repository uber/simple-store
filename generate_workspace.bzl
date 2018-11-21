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


  # com.squareup.leakcanary:leakcanary-android:aar:1.6.2
  native.maven_jar(
      name = "com_squareup_leakcanary_leakcanary_analyzer",
      artifact = "com.squareup.leakcanary:leakcanary-analyzer:1.6.2",
      repository = "https://jcenter.bintray.com/",
  )


  # com.squareup.leakcanary:leakcanary-analyzer:aar:1.6.2
  native.maven_jar(
      name = "com_squareup_haha_haha",
      artifact = "com.squareup.haha:haha:2.0.4",
      repository = "https://jcenter.bintray.com/",
      sha1 = "4dbe9405e87aa52687c692740253c0ef93dbad9b",
  )


  native.maven_jar(
      name = "com_squareup_leakcanary_leakcanary_android",
      artifact = "com.squareup.leakcanary:leakcanary-android:1.6.2",
      repository = "https://jcenter.bintray.com/",
  )


  # com.squareup.leakcanary:leakcanary-analyzer:aar:1.6.2
  native.maven_jar(
      name = "com_squareup_leakcanary_leakcanary_watcher",
      artifact = "com.squareup.leakcanary:leakcanary-watcher:1.6.2",
      repository = "https://jcenter.bintray.com/",
      sha1 = "5f24b0e8714f58109f791aa3db456dcb3c7d6704",
  )




def generated_java_libraries():
  native.java_library(
      name = "com_google_code_findbugs_jsr305",
      visibility = ["//visibility:public"],
      exports = ["@com_google_code_findbugs_jsr305//jar"],
  )


  native.java_library(
      name = "com_squareup_leakcanary_leakcanary_analyzer",
      visibility = ["//visibility:public"],
      exports = ["@com_squareup_leakcanary_leakcanary_analyzer//jar"],
      runtime_deps = [
          ":com_squareup_haha_haha",
          ":com_squareup_leakcanary_leakcanary_watcher",
      ],
  )


  native.java_library(
      name = "com_squareup_haha_haha",
      visibility = ["//visibility:public"],
      exports = ["@com_squareup_haha_haha//jar"],
  )


  native.java_library(
      name = "com_squareup_leakcanary_leakcanary_android",
      visibility = ["//visibility:public"],
      exports = ["@com_squareup_leakcanary_leakcanary_android//jar"],
      runtime_deps = [
          ":com_squareup_haha_haha",
          ":com_squareup_leakcanary_leakcanary_analyzer",
          ":com_squareup_leakcanary_leakcanary_watcher",
      ],
  )


  native.java_library(
      name = "com_squareup_leakcanary_leakcanary_watcher",
      visibility = ["//visibility:public"],
      exports = ["@com_squareup_leakcanary_leakcanary_watcher//jar"],
  )


