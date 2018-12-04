load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_android_library")

kt_android_library(
    name = "kotlin",
    srcs = glob([
        "*.kt",
    ]),
    deps = [
        "//java/com/uber/simplestore",
        "//java/com/uber/simplestore/impl",
        "@androidsdk//com.android.support:appcompat-v7-25.0.0",
    ],
    manifest = "AndroidManifest.xml",
    resource_files = glob(["res/**"]),
    visibility = ["//java/com/uber/simplestore/sample:__pkg__"],
)

