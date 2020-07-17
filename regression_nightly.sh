#!/usr/bin/env bash
cd "`dirname "$0"`"
configFile="./configs/$1"
echo $configFile

diskspace=$(df -h | grep xvda | awk '{print $5}' | cut -d'%' -f1)
if [ $diskspace -gt 85 ];
then
  echo "diskspace is too high at $diskspace%, removing logs from current test directory";
  cd results;  find ./ -type d -ctime +3 -exec rm -rf {} \;
fi


eval $(ssh-agent)
ssh-add /home/ubuntu/.ssh/regression_rsa

cd ..
git pull
git submodule update --init --merge
#make tempfile to store PIPESTATUS in from the mvnOutput command so that PIPESTATUS is preserved and the output can be logged in regreeion cron.err
# then save PIPESTATUS to RC and remove th etemp file
tmp=$(mktemp)
mvnOutput=$(mvn -DskipTests clean deploy | tee /dev/tty; echo "${PIPESTATUS[@]}" > "$tmp")
#mvn -DskipTests clean deploy
RC=($(<"$tmp"))
rm "$tmp"
if [ "${RC[0]}" -ne "0" ]
then
echo "MVN BUILD FAILED"
  slackMsg=$(echo "<!here> MVN Failed to run on regression nightly";echo;echo $mvnOutput| sed 's/\[/\n\[/g' | grep "\[ERROR\]")
  cd regression
#the is not a mvn build of regression here because a failed build in platform forces a failed build in regression
  java -cp regression.jar com.swirlds.regression.slack.SlackSubmodulePointerMsg "$slackMsg" "regression"
fi

cd regression
export aws_access_key_id=AKIAJJST62PF5EO3CMAQ
export aws_secret_access_key=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
java \
-Daws.accessKeyId=AKIAJJST62PF5EO3CMAQ \
-Daws.secretKey=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 \
-Dlog4j.configurationFile=log4j2-jrs.xml \
-Dspring.output.ansi.enabled=ALWAYS \
-jar regression.jar "$configFile"
