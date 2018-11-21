proto_library(
    name = "proto",
    srcs = glob(["*.proto"]),
)

java_lite_proto_library(
    name = "proto_lite",
    deps = [":proto"],
    visibility = ["//java/com/ubercab/simplestorage:__subpackages__"]
)