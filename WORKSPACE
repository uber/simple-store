load("//:generate_workspace.bzl", "generated_maven_jars")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

generated_maven_jars()

http_archive(
    name = "com_google_protobuf",
    sha256 = "cef7f1b5a7c5fba672bec2a319246e8feba471f04dcebfe362d55930ee7c1c30",
    strip_prefix = "protobuf-3.5.0",
    urls = ["https://github.com/google/protobuf/archive/v3.5.0.zip"],
)

http_archive(
    name = "com_google_protobuf_javalite",
    sha256 = "d8a2fed3708781196f92e1e7e7e713cf66804bd2944894401057214aff4f468e",
    strip_prefix = "protobuf-5e8916e881c573c5d83980197a6f783c132d4276",
    urls = ["https://github.com/google/protobuf/archive/5e8916e881c573c5d83980197a6f783c132d4276.zip"],
)

http_archive(
 name = "robolectric",
 urls = ["https://github.com/robolectric/robolectric-bazel/archive/4.0.1.tar.gz"],
 strip_prefix = "robolectric-bazel-4.0.1",
 sha256 = "dff7a1f8e7bd8dc737f20b6bbfaf78d8b5851debe6a074757f75041029f0c43b",
)
load("@robolectric//bazel:robolectric.bzl", "robolectric_repositories")
robolectric_repositories()

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

rules_kotlin_version = "87bd13f91d166a8070e9bbfbb0861f6f76435e7a"

http_archive(
    name = "io_bazel_rules_kotlin",
    urls = ["https://github.com/bazelbuild/rules_kotlin/archive/%s.zip" % rules_kotlin_version],
    type = "zip",
    strip_prefix = "rules_kotlin-%s" % rules_kotlin_version
)

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")
kotlin_repositories()
kt_register_toolchains()

android_sdk_repository(name = "androidsdk")
