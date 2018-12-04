android_binary(
    name = "sample",
    srcs = glob(["*.java"]),
    manifest = "AndroidManifest.xml",
    manifest_values = {
        "targetSdkVersion": "28",
        "minSdkVersion": "16",
    },
    deps = [
        "//java/com/uber/simplestore/sample/java",
        "//java/com/uber/simplestore/sample/kotlin",
        "@androidsdk//com.android.support:appcompat-v7-25.0.0",
    ],
)
