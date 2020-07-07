#!/usr/bin/env bash

eval $(ssh-agent)
ssh-add ~/.ssh/circlci-pointer-update-rsa

cd ..
echo -e "Host github.com\n\tStrictHostKeyChecking no\n" > ~/.ssh/config
# git pull
# git submodule update
echo $JAVA_HOME
# mvn -DskipTests clean deploy

regress_develop_commit=$(cd regression;git log -n 1 --decorate=short | head -n1 | awk '{print $2}';cd ..)
regress_sub_commit=$(git submodule status | grep regression | awk '{print $1}')

apps_develop_commit=$(cd platform-apps;git log -n 1 --decorate=short | head -n1 | awk '{print $2}';cd ..)
apps_sub_commit=$(git submodule status | grep platform-apps | awk '{print $1}')

if [ $regress_develop_commit != $regress_sub_commit ]
then
  regression_develop_information=$(cd regression;git log -n 1)
  slackMsg="regression commit doesn't equal develop sub module\nplatform regression pointer:$regress_sub_commit\n regression current commit:$regression_develop_information"
	echo $slackMsg
else
	echo "regresion is equal $regress_develop_commit == $regress_sub_commit"
fi

if [ $apps_develop_commit != $apps_sub_commit ]
then
	echo "apps commit doesn't equal develop sub module"
else
	echo "apps is equal"
fi

cd regression
#TODO delete this mvn deploy
mvn -DskipTests clean deploy
java -cp regression.jar com.swirlds.regression.slack.SlackSubmodulePointerMsg "$slackMsg"

