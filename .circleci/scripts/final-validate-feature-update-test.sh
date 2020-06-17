#!/usr/bin/env bash

# merge this with final-summary.sh

. ${REPO}/.circleci/scripts/utils.sh
. ${REPO}/.circleci/scripts/rationalize-tf-env-vars.sh

set +e
cd $REPO

TARGET_DIR=${1:-feature-update-logs}
BRANCH_DIR=${2:-${CIRCLE_BRANCH}}
CLIENT_LOG_DIR='client-logs'

ci_echo "Download all test client log files to ${CLIENT_LOG_DIR}..."
/usr/local/bin/aws s3 cp \
  s3://${SWIRLD_S3_BUCKET}/${TARGET_DIR}/${BRANCH_DIR} ${CLIENT_LOG_DIR} --recursive

echo "Summary of feature update test:" > ${CLIENT_LOG_DIR}/feature-update-test-summary.txt

python3 ${REPO}/.circleci/scripts/validate-feature-update-results.py  ${CLIENT_LOG_DIR}
