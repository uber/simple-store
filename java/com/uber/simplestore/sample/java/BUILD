android_library(
    name = "java",
    manifest = "AndroidManifest.xml",
    srcs = glob(["*.java"]),
    resource_files = glob(["res/**"]),
    deps = [
        "//java/com/uber/simplestore",
        "//java/com/uber/simplestore/executors",
        "//java/com/uber/simplestore/impl",
    ],
    visibility = ["//java/com/uber/simplestore/sample:__pkg__"],
)
