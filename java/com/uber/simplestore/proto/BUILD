android_library(
    name = "proto",
    srcs = glob(["*.java"]),
    visibility = ["//visibility:public"],
    deps = [
      "//java/com/uber/simplestore/impl",
      "//third_party:com_google_code_findbugs_jsr305",
      "//third_party:com_google_protobuf_protobuf_lite",
    ],
)

android_binary(
    name = "proto_bin",
    deps = [":proto"],
    manifest = "AndroidManifest.xml",
)
