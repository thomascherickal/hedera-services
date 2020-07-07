#!/usr/bin/env bash

eval $(ssh-agent)
ssh-add ~/.ssh/circlci-pointer-update-rsa

cd ..
echo -e "Host github.com\n\tStrictHostKeyChecking no\n" > ~/.ssh/config
# git pull
# git submodule update

regress_develop_commit=$(cd regression;git log -n 1 --decorate=short | head -n1 | awk '{print $2}';cd ..)
regress_sub_commit=$(git submodule status | grep regression | awk '{print $1}')

apps_develop_commit=$(cd platform-apps;git log -n 1 --decorate=short | head -n1 | awk '{print $2}';cd ..)
apps_sub_commit=$(git submodule status | grep platform-apps | awk '{print $1}')

if [ $regress_develop_commit != $regress_sub_commit ]
then
	echo "regression commit doesn't equal develop sub module"
else
	echo "regresion is equal $regress_develop_commit == $regress_sub_commit"
fi

if [ $apps_develop_commit != $apps_sub_commit ]
then
	echo "apps commit doesn't equal develop sub module"
else
	echo "apps is equal"
fi

