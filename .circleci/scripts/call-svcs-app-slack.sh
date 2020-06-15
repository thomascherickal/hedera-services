#!/usr/bin/env bash
set +e

. ${REPO}/.circleci/scripts/utils.sh

ensure_slackclient

# If the branch is master, adjust the hedera channel to default one.
if [ "${CIRCLE_BRANCH}" = "master" ]
then
  final_args="$* -c hedera-regression"
else
  final_args="$*"
fi

echo "final_args: $final_args"

SLACK_API_TOKEN=xoxb-344480056389-890228995125-MfvKfFwJtL0ba2Ms8HDf8656 \
   python3 ${REPO}/.circleci/scripts/svcs-app-slack.py $final_args
