#!/usr/bin/env bash

eval $(ssh-agent)
ssh-add ~/.ssh/circlci-pointer-update-rsa

cd ..
echo -e "Host github.com\n\tStrictHostKeyChecking no\n" > ~/.ssh/config
git fetch
# git submodule update
# mvn -DskipTests clean deploy

regress_develop_commit=$(cd regression;git log -n 1 --remotes | head -n1 | awk '{print $2}';cd ..)
# regress_sub_commit=$(git log -n 1 --submodule regression | head -n1 | awk '{print $2}')
regress_sub_commit=$(git submodule status | grep regression | awk '{print $1}')

apps_develop_commit=$(cd platform-apps;git log -n 1 --remotes | head -n1 | awk '{print $2}';cd ..)
# apps_sub_commit=$(git log -n 1 --submodule platform-apps | head -n1 | awk '{print $2}')
apps_sub_commit=$(git submodule status | grep platform-apps | awk '{print $1}')

if [ $regress_develop_commit != $regress_sub_commit ]
then
  regression_develop_information=$(cd regression;git log -n 1 --remotes)
  slackMsg="regression commit doesn't equal develop sub module
  *platform regression pointer:* $regress_sub_commit
  *regression current commit:*
  $regression_develop_information"
	echo $slackMsg
fi

if [ $apps_develop_commit != $apps_sub_commit ]
then
	apps_develop_information=$(cd platform-apps;git log -n 1 --remotes)
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

