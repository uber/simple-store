android_library(
    name = "simplestorage",
    visibility = ["//visibility:public"],
    srcs = glob(["*.java"]),
    deps = [
            "//third_party:com_google_code_findbugs_jsr305",
    ],
)

java_binary(
    name = "simplestorage_bin",
    runtime_deps = ["//java/com/ubercab/simplestorage/impl",
            "//java/com/ubercab/simplestorage/proto:proto_lite",
                    ],
)
