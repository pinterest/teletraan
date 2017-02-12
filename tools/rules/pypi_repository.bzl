# Installation requirements prior to using this rule:
# 1) pip binary: `pip`
# 2) For pypi package with C extensions or system dependecies, 
#    make sure to build on host with same setup or build in docker

# Binary dependencies needed for pypi repo setup
DEPS = ["pip", "sed", "basename"]

def _execute(ctx, command):
  return ctx.execute(["bash", "-c", """
set -ex
%s""" % command])

def _check_dependencies(ctx):
  for dep in DEPS:
    if ctx.which(dep) == None:
      fail("%s requires %s as a dependency. Please check your PATH." % (ctx.name, dep))

_pypi_repository_template = """
# DO NOT EDIT: automatically generated BUILD file for pypi_repository rule

package(default_visibility = ["//visibility:public"])

{pypi_packages}
"""

_pypi_package_template = """
filegroup(
    name = '{name}',
    srcs = {glob},
)"""

def _install_package(repository_ctx, package_ctx):
    # Install package into specific target
    print("install %s..." % package_ctx.artifact)
    command = "pip install --no-deps --ignore-installed --target={target} {artifact}".format(
        target=package_ctx.target,
        artifact=package_ctx.artifact,
    )
    if repository_ctx.attr.conf:
       conf = repository_ctx.path(repository_ctx.conf)
       command = "PIP_CONFIG_FILE={conf} {pipinstall}".format(
           conf=conf,
           pipinstall=command,
       )

    result = _execute(repository_ctx, command)
    if result.return_code != 0:
      fail("%s\n%s\nFailed to install package %s" % (result.stdout, result.stderr, package_ctx.artifact))

    # Generate signature file, which will be used to decide if install is necessary next time
    result = _execute(repository_ctx, "echo '%s' > %s" % (package_ctx.artifact, package_ctx.signature))
    if result.return_code != 0:
      fail("%s\n%s\nFailed to generate signature for %s" % (result.stdout, result.stderr, package_ctx.artifact))

def _create_symlinks(repository_ctx, package_ctx):
    # Create symlinks
    result = _execute(repository_ctx, "cd %s && ln -s %s/* ." %(package_ctx.current, package_ctx.target))
    if result.return_code != 0:
        fail("%s\n%s\nFailed to create symlinks for %s" % (result.stdout, result.stderr, package_ctx.artifact))

def _uninstall_package(repository_ctx, package_ctx):
    # delete all the symlinks
    command = "find %s -maxdepth 1 -mindepth 1 -exec basename {} \;" % package_ctx.target
    files_str = _execute(repository_ctx, command).stdout
    for file_str in files_str.splitlines():
        _execute(repository_ctx, "rm -fr %s/%s" % (package_ctx.current, file_str))

    # delete package_ctx.target dir
    _execute(repository_ctx, "rm -fr %s" % package_ctx.target)

    # delete signature file
    _execute(repository_ctx, "rm -fr %s" % package_ctx.signature)

def _dir_exists(repository_ctx, path):
    return _execute(repository_ctx, "[[ -d %s ]] && exit 0 || exit 1" % path).return_code == 0

def _file_exists(repository_ctx, path):
    return _execute(repository_ctx, "[[ -f %s ]] && exit 0 || exit 1" % path).return_code == 0

def _need_install(repository_ctx, package_ctx):
    if not _dir_exists(repository_ctx, package_ctx.target):
        return True

    if _file_exists(repository_ctx, package_ctx.signature):
        signature = _execute(repository_ctx, "cat %s"% package_ctx.signature).stdout
        if signature:
            signature = signature.strip("\n")
        if package_ctx.artifact == signature:
            # signature matched, skip install
            return False

    _uninstall_package(repository_ctx, package_ctx)
    return True

def _generate_glob_content(repository_ctx, package_ctx):
    # return something like glob(['nose_socket_whitelist-1.0.0.egg-info', 'socketwhitelist/**'])
    result = "glob(["

    command = "find %s -maxdepth 1 -mindepth 1 -type f -exec basename {} \;" % package_ctx.target
    files_str = _execute(repository_ctx, command).stdout
    for file_str in files_str.splitlines():
        result += "'%s'," % file_str      

    command = "find %s -maxdepth 1 -mindepth 1 -type d -exec basename {} \;" % package_ctx.target
    dirs_str = _execute(repository_ctx, command).stdout
    for dir_str in dirs_str.splitlines():
        result += "'%s/**'," % dir_str      

    return result + "])"

def _create_package_ctx(repository_ctx, artifact):
    # expect artifact in the format of: pytest==2.3.5, nose-socket-whitelist, mock>=1.0.1 etc.
    # convert to bazel rule name such as pytest, nose_socket_whitelist, mock etc.
    name = _execute(repository_ctx, "echo '%s' | sed 's/[=><].*//'" % artifact).stdout
    name = name.strip("\n").replace("-", "_")

    # repo root dir
    repo = repository_ctx.path(repository_ctx.attr.repo)
    repo = _execute(repository_ctx, "dirname %s" % repo).stdout.strip("\n")

    return struct(
      # package name, such as nose_socket_whitelist
      name = name,
      # package artifact, such as pytest==2.3.5
      artifact = artifact,
      repo = repo,
      # package actual install directory, such as thirdparty/python/pypirepo/_mock/
      target = "%s/%s" % (repo, name),
      # package signature file path, such as thirdparty/python/pypirepo/_mock.sig
      signature = "%s/%s.sig" % (repo, name),
      # current directory, such as <BAZEL_EXTERNAL>/pypi/
      current = repository_ctx.path("."),
    )

def _pypi_repository_impl(repository_ctx):
    # Ensure that we have all of the dependencies installed
    _check_dependencies(repository_ctx)

    # Install all the packages and generate all the filegroup rules
    filegroup_rule_contents = [] 
    for package in repository_ctx.attr.packages:
        package_ctx = _create_package_ctx(repository_ctx, package)
        if _need_install(repository_ctx, package_ctx):
            _install_package(repository_ctx, package_ctx)
        _create_symlinks(repository_ctx, package_ctx)
        glob_content = _generate_glob_content(repository_ctx, package_ctx)
        filegroup_rule_contents.append(_pypi_package_template.format(name=package_ctx.name, glob=glob_content))

    # Create final build file
    pypi_repo_build_content = _pypi_repository_template.format(pypi_packages="\n".join(filegroup_rule_contents))
    repository_ctx.file('BUILD', pypi_repo_build_content, False)

pypi_repository = repository_rule(
    implementation=_pypi_repository_impl,
    attrs={
        # Provide customized pip.conf as needed, default is pip system defaults.
        "conf": attr.label(default = None),
        # A complete list of pypi packages, including transitive ones, is required in this single file.
        # Packages are in the format of: pytest==2.3.5, nose-socket-whitelist, mock>=1.0.1 etc.
        "packages": attr.string_list(default = []),
        # pypi repo root
        "repo": attr.label(default=Label("//thirdparty/python:.pypirepo/placeholder.txt")),
    },
    local=False,
)
