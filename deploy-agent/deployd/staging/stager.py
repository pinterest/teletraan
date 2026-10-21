# Copyright 2016 Pinterest, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import argparse
import os
from pwd import getpwnam
import re
import sys
import shutil
import traceback
import logging
from typing import Optional

from deployd.common import LOG_FORMAT
from deployd.common.caller import Caller
from deployd.common.config import Config
from deployd.common.status_code import Status
from deployd.common.stats import create_sc_increment
from .transformer import Transformer

log = logging.getLogger(__name__)

# T036: TELETRAAN_* variable substitution only runs on files that live under
# the `teletraan_template/` directory. Tokens in other locations are silently
# left unsubstituted and show up much later as cryptic runtime failures.
# _TELETRAAN_TOKEN_RE matches both ${TELETRAAN_NAME}, {$TELETRAAN_NAME}, and
# bare $TELETRAAN_NAME usages so we can warn at deploy time.
_TELETRAAN_TOKEN_RE = re.compile(r"\$\{?TELETRAAN_[A-Za-z0-9_\-]+")
# Extensions we are willing to scan for tokens. Large binary files in the
# build directory are not worth reading; keep the list to config/script-ish
# files to stay cheap and avoid false positives in compiled assets.
_TELETRAAN_SCAN_EXTENSIONS = (
    "",
    ".sh",
    ".bash",
    ".zsh",
    ".py",
    ".rb",
    ".pl",
    ".conf",
    ".cfg",
    ".ini",
    ".properties",
    ".yaml",
    ".yml",
    ".json",
    ".env",
    ".txt",
    ".service",
    ".xml",
    ".tmpl",
    ".template",
)
# Cap per-deploy to avoid walking extremely large build trees; the warning is
# best-effort and only needs to catch the top offenders.
_TELETRAAN_SCAN_MAX_FILES = 2000
_TELETRAAN_SCAN_MAX_WARNINGS = 50
_TELETRAAN_SCAN_MAX_FILE_BYTES = 1 * 1024 * 1024  # 1 MiB per file


class Stager(object):
    _script_dirname = "teletraan"
    _template_dirname = "teletraan_template"

    def __init__(self, config, build, target, env_name, transformer=None) -> None:
        self._build_dir = config.get_builds_directory()
        self._user_role = config.get_user_role()
        agent_dir = config.get_agent_directory()
        self._transformer = transformer or Transformer(
            agent_dir=agent_dir, env_name=env_name
        )
        self._build = build
        self._target = target
        self._env_name = env_name

    def enable_package(self) -> int:
        """Set the enabled build."""
        old_build = self.get_enabled_build()
        if self._build == old_build:
            self.transform_script()
            log.info("Build already at {}".format(self._build))
            return Status.SUCCEEDED

        old_build = self.get_enabled_build()
        build_dir = os.path.join(self._build_dir, self._build)
        # Make a tmp_symlink
        tmp_symlink = "{}_tmp".format(self._target)
        if os.path.exists(tmp_symlink):
            os.remove(tmp_symlink)

        status_code = Status.SUCCEEDED
        try:
            # change the owner of the directory
            uinfo = getpwnam(self._user_role)
            owner = "{}:{}".format(uinfo.pw_uid, uinfo.pw_gid)
            commands = ["chown", "-R", owner, build_dir]
            log.info("Running command: {}".format(" ".join(commands)))
            output, error, status = Caller().call_and_log(commands)
            if status != 0:
                log.error(error)
                return Status.FAILED

            # setup symlink
            os.symlink(build_dir, tmp_symlink)
            # Move tmp_symlink over existing symlink.
            os.rename(tmp_symlink, self._target)
            log.info(
                "{} points to {} (previously {})".format(
                    self._target, self.get_enabled_build(), old_build
                )
            )
            self.transform_script()
        except Exception:
            log.error(traceback.format_exc())
            create_sc_increment(
                name="deploy.failed.stager.symlink",
                tags={"env": self._env_name, "build": self._build},
            )
            status_code = Status.FAILED
        finally:
            return status_code

    def get_enabled_build(self) -> Optional[str]:
        """Figure out what build is enabled by looking at symlinks."""
        if not os.path.exists(self._target):
            if os.path.islink(self._target) and not os.path.lexists(self._target):
                symlink_target = os.readlink(self._target)
                log.info(
                    "{} points to {} which does not exist".format(
                        self._target, symlink_target
                    )
                )
            else:
                log.info("{} does not exist".format(self._target))
            return None

        if not os.path.islink(self._target):
            log.info("{} is not a symlink".format(self._target))
            return None

        symlink_target = os.readlink(self._target)

        return symlink_target.rsplit("/", 1)[-1]

    def transform_script(self) -> None:
        script_dir = os.path.join(self._target, self._script_dirname)
        if not os.path.exists(script_dir):
            return

        template_dir = os.path.join(self._target, self._template_dirname)
        # copy user script template to a dedicated template directory
        if not os.path.exists(template_dir):
            shutil.copytree(script_dir, template_dir)

        self._transformer.transform_scripts(
            script_dir=template_dir,
            template_dirname=self._template_dirname,
            script_dirname=self._script_dirname,
        )

        # T036: warn (best-effort) when $TELETRAAN_* tokens appear in files
        # that live outside the teletraan_template/ directory — those tokens
        # are NOT substituted by the Transformer and will reach the runtime
        # as literal strings.
        try:
            self._warn_on_unsubstituted_teletraan_tokens(template_dir)
        except Exception:
            log.debug(
                "TELETRAAN_ token scan failed; continuing: %s",
                traceback.format_exc(),
            )

    def _warn_on_unsubstituted_teletraan_tokens(self, template_dir: str) -> None:
        """Walk the build target and warn if $TELETRAAN_* tokens appear
        outside ``teletraan_template/``.

        Only those tokens get substituted by the Transformer, so any
        occurrence elsewhere is a silent no-op that users typically discover
        at runtime via a cryptic symptom. Logging at deploy time gives a
        much clearer breadcrumb (T036).
        """
        if not self._target or not os.path.isdir(self._target):
            return

        template_dir_abs = os.path.abspath(template_dir)
        files_scanned = 0
        warnings_emitted = 0
        for root, dirs, files in os.walk(self._target, followlinks=False):
            # Skip the template directory itself — tokens there are expected
            # and will be substituted.
            root_abs = os.path.abspath(root)
            if (
                root_abs == template_dir_abs
                or root_abs.startswith(template_dir_abs + os.sep)
            ):
                dirs[:] = []
                continue

            for filename in files:
                if files_scanned >= _TELETRAAN_SCAN_MAX_FILES:
                    return
                if warnings_emitted >= _TELETRAAN_SCAN_MAX_WARNINGS:
                    log.warning(
                        "TELETRAAN_ token scan: suppressing further warnings "
                        "after %d hits (emit cap). Inspect files outside "
                        "teletraan_template/ for remaining $TELETRAAN_* tokens.",
                        warnings_emitted,
                    )
                    return
                ext = os.path.splitext(filename)[1].lower()
                if ext not in _TELETRAAN_SCAN_EXTENSIONS:
                    continue
                fpath = os.path.join(root, filename)
                try:
                    size = os.path.getsize(fpath)
                except OSError:
                    continue
                if size == 0 or size > _TELETRAAN_SCAN_MAX_FILE_BYTES:
                    continue
                files_scanned += 1
                try:
                    with open(fpath, "r", errors="replace") as fh:
                        content = fh.read()
                except (OSError, UnicodeDecodeError):
                    continue
                matches = _TELETRAAN_TOKEN_RE.findall(content)
                if not matches:
                    continue
                unique_tokens = sorted(set(matches))
                log.warning(
                    "Found unsubstituted TELETRAAN_ token(s) %s in %s — this "
                    "file is OUTSIDE teletraan_template/ so variables were "
                    "NOT substituted and will reach runtime as literals. "
                    "Move the file under teletraan_template/ or export the "
                    "value from a RESTARTING script. (T036)",
                    unique_tokens[:5],
                    fpath,
                )
                warnings_emitted += 1


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "-f",
        "--config-file",
        dest="config_file",
        default=None,
        help="the deploy agent conf file filename path. If none, "
        "/etc/deployagent.conf will be used",
    )
    parser.add_argument(
        "-v",
        "--build-id",
        dest="build",
        required=True,
        help="the current deploying build version for the current environment.",
    )
    parser.add_argument(
        "-t",
        "--target",
        dest="target",
        required=True,
        help="The deploy target directory name.",
    )
    parser.add_argument(
        "-e",
        "--env-name",
        dest="env_name",
        required=True,
        help="the environment name currently in deploy.",
    )
    args = parser.parse_args()
    config = Config(args.config_file)
    logging.basicConfig(format=LOG_FORMAT, level=config.get_log_level())

    log.info("Start to stage the package.")
    result = Stager(
        config=config, build=args.build, target=args.target, env_name=args.env_name
    ).enable_package()
    if result == Status.SUCCEEDED:
        return 0
    else:
        create_sc_increment(
            name="deploy.failed.stager.enable_package",
            tags={"env": args.env_name, "build": args.build},
        )
        return 1


if __name__ == "__main__":
    sys.exit(main())
