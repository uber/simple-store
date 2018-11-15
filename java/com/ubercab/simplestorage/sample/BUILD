android_binary(
    name = "sample",
    srcs = glob(["*.java"]),
    manifest = "AndroidManifest.xml",
    manifest_values = {
        "targetSdkVersion": "28",
        "minSdkVersion": "28",
    },
    resource_files = glob(["res/**"]),
    deps = [
        "//java/com/ubercab/simplestorage",
        "//java/com/ubercab/simplestorage/impl",
        "@androidsdk//com.android.support:appcompat-v7-25.0.0",
    ],
)
