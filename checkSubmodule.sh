#!/usr/bin/env bash

eval $(ssh-agent)
ssh-add ~/.ssh/circlci-pointer-update-rsa

cd ..
echo -e "Host github.com\n\tStrictHostKeyChecking no\n" > ~/.ssh/config
git fetch
git pull
git submodule update --force
regress_sub_commit=$(git submodule status | grep regression | awk '{print $1}')
apps_sub_commit=$(git submodule status | grep platform-apps | awk '{print $1}')
# mvn -DskipTests clean deploy
cd regression
git checkout origin/P2161-automatePointerUpdate
regress_develop_commit=$(git log -n 1 | head -n1 | awk '{print $2}')
cd ..
# regress_sub_commit=$(git log -n 1 --submodule regression | head -n1 | awk '{print $2}')

cd platform-apps
git checkout origin/develop
apps_develop_commit=$(git log -n 1 | head -n1 | awk '{print $2}')
cd ..
# apps_sub_commit=$(git log -n 1 --submodule platform-apps | head -n1 | awk '{print $2}')


if [ $regress_develop_commit != $regress_sub_commit ]
then
  regression_develop_information=$(cd regression;git log -n 1)
  slackMsg="regression commit doesn't equal develop sub module
  *platform regression pointer:* $regress_sub_commit
  *regression current commit:*
  $regression_develop_information"
	echo $slackMsg
fi

if [ $apps_develop_commit != $apps_sub_commit ]
then
	apps_develop_information=$(cd platform-apps;git log -n 1)
  slackMsg="$slackMsg

  platform-apps commit doesn't equal develop sub module
  *platform platform-apps pointer:* $apps_sub_commit
  *platform-apps current commit:*
  $apps_develop_information"
	echo $slackMsg
fi

if [ -n "$slackMsg" ]
then
  cd regression
  #TODO delete this mvn deploy
  mvn -DskipTests clean deploy
  java -cp regression.jar com.swirlds.regression.slack.SlackSubmodulePointerMsg "$slackMsg"
fi

