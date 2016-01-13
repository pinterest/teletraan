import glob
import logging
import os
import shutil

log = logging.getLogger(__name__)


class Helper(object):

    def __init__(self, config=None):
        self._config = config

    def builds_available_locally(self, builds_dir):
        """Returns a list of (build, timestamp) that we have installed."""
        builds = []
        try:
            for filename in os.listdir(builds_dir):
                path = os.path.join(builds_dir, filename)
                if os.path.isdir(path):  # We only care about the actual builds.
                    builds.append((filename, os.path.getmtime(path)))
        except OSError:
            # if builds_dir doesn't exist, there is no local build,
            # go on and return empty list.
            log.debug("OSError: {} does not exist.".format(builds_dir))
        finally:
            return builds

    @staticmethod
    def get_stale_builds(build_timestamps, num_builds_to_retain=2):
        """
        Given a list of (build, timestamp) tuples, determine which are stale.

        :param num_builds_to_retain: number of builds to keep at a minimum.

        """
        # Sorted by timestamp, oldest first.
        sorted_items = sorted(build_timestamps, key=lambda bt: bt[1])
        total_builds = len(sorted_items)
        yielded_builds = 0

        for item in sorted_items:
            if total_builds - yielded_builds <= num_builds_to_retain:
                break
            build, timestamp = item

            yield build
            yielded_builds += 1

    @staticmethod
    def clean_package(base_dir, builds, build_name):
        local_fn = '{}-{}.*'.format(build_name, builds)
        try:
            extracted_file = os.path.join(base_dir, '{}.extracted'.format(builds))
            if os.path.exists(extracted_file):
                os.remove(extracted_file)
            staged_file = os.path.join(base_dir, '{}.staged'.format(builds))
            if os.path.exists(staged_file):
                os.remove(staged_file)
        except OSError as e:
            log.error(e)

        try:
            shutil.rmtree(os.path.join(base_dir, builds))
            fns = glob.glob(os.path.join(base_dir, local_fn))
            if fns:
                os.remove(fns[0])
        except OSError as e:
            log.error(e)
