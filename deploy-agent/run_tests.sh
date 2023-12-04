#/usr/bin/sh

export PYTHONPATH=.
./tests/run_unit_tests.sh
./tests/run_integ_tests.sh
