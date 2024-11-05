Deploy-agent is a python script runs on every host and execute deploy scripts.
See https://github.com/pinterest/teletraan/wiki for more details.

# Before you commit new code

1. Install [pre-commit](https://pre-commit.com/#install)
```bash
cd teletraan
pip install pre-commit
pre-commit install
```

# Using Bazel
## Prerequisites
Ensure that your python version is at least python3.8.

## Building
```bash
cd teletraan/deploy-agent/
sudo bazel build //deployd:deploy-agent
```

## Testing
```bash
cd teletraan/deploy-agent/
sudo bazel test //tests:*

```

# FAQ

## Run pre-commit on the last commit only
```bash
cd teletraan
pre-commit run --files $(git diff --name-only HEAD^1...HEAD)
```

## To run unit tests (debian comapatible):

1. Install `tox` with apt.
2.  `> tox`
