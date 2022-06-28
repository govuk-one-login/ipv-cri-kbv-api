#!/usr/bin/env bash
set -eu
REPORT_DIR="${TEST_REPORT_DIR:=$PWD}"
echo aws-cli version is "$(aws --version)"

IPV_CORE_STUB_START_URL=$PIPELINE_START_URL \
IPV_CORE_STUB_AUTH_USERNAME=$PIPELINE_BASIC_AUTH_USERNAME \
IPV_CORE_STUB_AUTH_PASSWORD=$PIPELINE_BASIC_AUTH_PASSWORD \
gradle -q test
cp -r build/test-results/* "$REPORT_DIR"
