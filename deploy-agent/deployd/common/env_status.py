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

import errno
import json
import logging
import lockfile
import os
import shutil
import traceback

from deployd import IS_PINTEREST
from deployd.common.types import DeployStatus
from deployd.common.utils import touch

log = logging.getLogger(__name__)

# Errnos that indicate the host is out of disk space or quota. When we hit one
# of these while writing the status file, surface disk stats at ERROR so the
# oncall does not chase the downstream misleading ImageNotFound / retry
# failure modes (see T002: 'Failed to dump status to the disk' is the strongest
# on-host signal that disk is full).
_DISK_FULL_ERRNOS = frozenset(
    filter(
        None,
        (
            getattr(errno, "ENOSPC", None),
            getattr(errno, "EDQUOT", None),
            getattr(errno, "EFBIG", None),
        ),
    )
)


class EnvStatus(object):
    def __init__(self, status_fn) -> None:
        self._status_fn = status_fn
        self._lock_fn = "{}.lock".format(self._status_fn)
        self._lock = lockfile.FileLock(self._lock_fn)

    def load_envs(self) -> dict:
        """
        open up config file
        validate that the service selected exists
        """
        envs = {}
        try:
            with self._lock, open(self._status_fn, "r+") as config_fn:
                data = json.load(config_fn)
                log.debug("load status file: {}".format(data))
                envs = {key: DeployStatus(json_value=d) for key, d in data.items()}
        except IOError:
            log.info(
                "Could not find file {}. It happens when run deploy-agent the "
                "first time, or there is no deploy yet.".format(self._status_fn)
            )
            return {}
        except Exception:
            log.exception("Something went wrong in load_envs")
        finally:
            return envs

    def _touch_or_rm_host_type_file(
        self, envs, host_type, directory="/var/run/"
    ) -> None:
        """Touches or removes the identity file for the host type.
        For now, a host type could be 'canary'.
        """
        host_type_match = False
        file_path = os.path.join(directory, host_type)
        for key, value in envs.items():
            if value.report.stageName == host_type:
                host_type_match = True
                break

        if host_type_match:
            log.debug("The host is a {}.".format(host_type))
            if not os.path.isfile(file_path):
                touch(file_path)
                log.debug("Touched {}.".format(file_path))
        else:
            log.debug("The host is not a {}.".format(host_type))
            if os.path.isfile(file_path):
                os.remove(file_path)
                log.debug("Removed {}.".format(file_path))

    def dump_envs(self, envs) -> bool:
        try:
            json_data = {}
            if envs:
                json_data = {key: value.to_json() for key, value in envs.items()}
            with self._lock, open(self._status_fn, "w") as config_output:
                json.dump(
                    json_data,
                    config_output,
                    sort_keys=True,
                    indent=2,
                    separators=(",", ": "),
                )

            if IS_PINTEREST:
                self._touch_or_rm_host_type_file(envs, "canary")
            return True
        except IOError as e:
            self._log_dump_failure(e)
            return False
        except Exception:
            log.error(traceback.format_exc())
            return False

    def _log_dump_failure(self, exc) -> None:
        """Emit a diagnostic log line for a failed status dump.

        When the failure is disk-full (ENOSPC / EDQUOT), escalate to ERROR and
        attach free/used/total bytes for the mount containing the status file.
        This is the canonical on-host signal for host disk exhaustion, which
        otherwise surfaces downstream as a misleading Docker 'ImageNotFound'.
        """
        errno_value = getattr(exc, "errno", None)
        try:
            probe_path = os.path.dirname(self._status_fn) or self._status_fn
            usage = shutil.disk_usage(probe_path)
            disk_info = (
                " probe_path=%s disk_total_bytes=%d disk_used_bytes=%d "
                "disk_free_bytes=%d"
            ) % (probe_path, usage.total, usage.used, usage.free)
        except Exception as probe_exc:  # disk_usage best-effort
            disk_info = " probe_path=%s disk_probe_failed=%s" % (
                self._status_fn,
                probe_exc,
            )

        if errno_value in _DISK_FULL_ERRNOS:
            log.error(
                "Failed to dump status to the disk (disk-full): status_fn=%s "
                "errno=%s reason=%s%s. Subsequent deploys on this host will "
                "likely surface as misleading Docker ImageNotFound errors until "
                "disk is reclaimed.",
                self._status_fn,
                errno_value,
                exc,
                disk_info,
            )
        else:
            log.warning(
                "Could not write to %s. Reason: %s errno=%s%s",
                self._status_fn,
                exc,
                errno_value,
                disk_info,
            )
