#!/usr/bin/env bash
cd "`dirname "$0"`"
configFile="./configs/$1"
echo $configFile

diskspace=$(df -h | grep xvda | awk '{print $5}' | cut -d'%' -f1)
if [ $diskspace -gt 85 ];
then
  echo "diskspace is too high at $diskspace%, removing logs from current test directory";
  cd results;  find ./ -type d -ctime +3 -exec rm {} \;

else
  echo "We're safe $diskspace";
fi


eval $(ssh-agent)
ssh-add /home/ubuntu/.ssh/regression_rsa

cd ..
git pull
git submodule update --init --merge
mvn -DskipTests clean deploy

cd regression
export aws_access_key_id=AKIAJJST62PF5EO3CMAQ
export aws_secret_access_key=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
java \
-Daws.accessKeyId=AKIAJJST62PF5EO3CMAQ \
-Daws.secretKey=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 \
-Dlog4j.configurationFile=log4j2-jrs.xml \
-Dspring.output.ansi.enabled=ALWAYS \
-jar regression.jar "$configFile"
