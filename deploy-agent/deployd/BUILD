load("@rules_python//python:defs.bzl", "py_test", "py_library")
load("@python_deps//:requirements.bzl", "requirement")

py_library(
    name = "lib",
    srcs = glob(["**/*.py"]),
    deps = [
        ":third_party",
    ],
    visibility = ["//tests:__pkg__"],
)

py_binary(
    name = "deploy-agent",
    srcs = ["agent.py"],
    main = "agent.py",
    deps = [
       ":lib",
    ],
)

py_binary(
    name = "deploy-downloader",
    srcs = ["download/downloader.py"],
    main = "download/downloader.py",
    deps = [
       ":lib",
    ],
)

py_library(
  name = "third_party",
  deps = [
    requirement("PyYAML"),
    requirement("zipp"),
    requirement("configparser"),
    requirement("python-daemon"),
    requirement("setuptools"),
    requirement("pinstatsd"),
    requirement("strictyaml"),
  ],
)