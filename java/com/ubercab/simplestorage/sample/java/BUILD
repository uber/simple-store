android_library(
    name = "java",
    manifest = "AndroidManifest.xml",
    srcs = glob(["*.java"]),
    resource_files = glob(["res/**"]),
    deps = [
        "//java/com/ubercab/simplestorage",
        "//java/com/ubercab/simplestorage/executors",
        "//java/com/ubercab/simplestorage/impl",
    ],
    visibility = ["//java/com/ubercab/simplestorage/sample:__pkg__"],
)