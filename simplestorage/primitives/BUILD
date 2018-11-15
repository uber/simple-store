proto_library(
    name = "primitives",
    srcs = glob(["*.proto"]),
)

java_lite_proto_library(
    name = "primitives_lite",
    deps = [":primitives"],
    visibility = ["//java/com/ubercab/simplestorage:__subpackages__"]
)