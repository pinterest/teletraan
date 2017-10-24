# Installation requirements prior to using this rule:
# 1) pip binary: `pip`
# 2) For pypi package with C extensions or system dependecies, 
#    make sure to build on host with same setup or build in docker

# Binary dependencies needed for pypi repo setup
DEPS = ["pip"]

def _execute(ctx, command):
  return ctx.execute(["bash", "-c", """
set -ex
%s""" % command])

def _check_dependencies(ctx):
  for dep in DEPS:
    if ctx.which(dep) == None:
      fail("%s requires %s as a dependency. Please check your PATH." % (ctx.name, dep))

_pypi_package_template = """
# DO NOT EDIT: automatically generated BUILD file for pypi_package rule
{filegroup}
"""

_filegroup_template = """
filegroup(
    name = 'lib',
    srcs = {glob},
    visibility = ["//visibility:public"],
)"""

def _install_package(repository_ctx):
    target = repository_ctx.path(".")
    artifact = repository_ctx.attr.artifact
    command = "pip install --no-deps --ignore-installed --target=%s %s" % (target, artifact)
    if repository_ctx.attr.conf:
       conf = repository_ctx.path(repository_ctx.conf)
       command = "PIP_CONFIG_FILE=%s %s" % (conf, command)

    result = _execute(repository_ctx, command)
    if result.return_code != 0:
      fail("%s\n%s\nFailed to install package %s" % (result.stdout, result.stderr, artifact))

def _generate_glob_content(repository_ctx):
    return "glob(['**/*'], exclude = ['**/=*'],)"

def _pypi_package_impl(repository_ctx):
    # Ensure that we have all of the dependencies installed
    _check_dependencies(repository_ctx)

    # Install the package
    _install_package(repository_ctx)
    glob_content = _generate_glob_content(repository_ctx)
    filegroup_content = _filegroup_template.format(glob=glob_content)

    # Create build file
    build_content = _pypi_package_template.format(filegroup=filegroup_content)
    repository_ctx.file('BUILD', build_content, False)

pypi_package = repository_rule(
    implementation=_pypi_package_impl,
    attrs={
        # Provide customized pip.conf as needed, default is pip system defaults.
        "conf": attr.label(default=None),
        # Package artifact in the format of: pytest==2.3.5, nose-socket-whitelist, mock>=1.0.1 etc.
        "artifact": attr.string(mandatory=True),
    },
    local=False,
)

