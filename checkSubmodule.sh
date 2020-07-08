#!/usr/bin/env bash

############################################################################
# Due to the nature of sub-modules this script much first over write what is currently in the sub-modules and then
# check out the develop branch of both in order to reliably find out if the sub-module pointers are pointing to the
# most current version of develop. It may be best to move this into a constant intigration package to ensure this is
# always done with a clean copy.
#
###########################################################################

# turn on the ssh0agent and make sure the proper ssh key is loaded for git activities
eval $(ssh-agent)
ssh-add ~/.ssh/circlci-pointer-update-rsa

# move to the main platform directory, make sure strict host key checking is off so git works
# pull the latest code, then force the submodule to update. This will make them on "no-branch" which makes checking logs
# for a specific branch such as develop impossible
cd ..
echo -e "Host github.com\n\tStrictHostKeyChecking no\n" > ~/.ssh/config
git fetch
git pull
git submodule update --force
# get the current sub-module commits now, as moving them to the develop brnach will change the status and give us false
# commits
regress_sub_commit=$(git submodule status | grep regression | awk '{print $1}')
apps_sub_commit=$(git submodule status | grep platform-apps | awk '{print $1}')

# change regression to the develop branch and get the latest commit
cd regression
git checkout origin/develop
regress_develop_commit=$(git log -n 1 | head -n1 | awk '{print $2}')
cd ..
# regress_sub_commit=$(git log -n 1 --submodule regression | head -n1 | awk '{print $2}')

# change platform-apps to the develop branch and get the latest commit
cd platform-apps
git checkout origin/develop
apps_develop_commit=$(git log -n 1 | head -n1 | awk '{print $2}')
cd ..


#build up message to be slacked
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

#if the message exists slack it.
if [ -n "$slackMsg" ]
then
  #make sure the most current regression.jar is built to the message can be sent.
  cd regression
  mvn -DskipTests clean deploy
  java -cp regression.jar com.swirlds.regression.slack.SlackSubmodulePointerMsg "$slackMsg"
fi

