#!/bin/bash

./remoteRun.sh my-key.pem clientIP.txt ubuntu "cd hedera-services/test-clients;  git checkout src/main/resource/spec-default.properties; git checkout hcs-submit-random-msg;  git reset --hard HEAD ;git pull;"


./remoteRun.sh my-key.pem clientIP.txt ubuntu cmdFile.txt serverIP.txt
./remoteRun.sh my-key.pem clientIP.txt ubuntu cmdFile2.txt serverIP.txt

# ./remoteRun.sh my-key.pem clientIP.txt ubuntu "sed -i 's/tls=off/tls=alternate/g' hedera-services/test-clients/src/main/resource/spec-default.properties "

./remoteRun.sh my-key.pem clientIP.txt ubuntu "sed -i 's/status.wait.timeout.ms=5000/status.wait.timeout.ms=30000/g' hedera-services/test-clients/src/main/resource/spec-default.properties "

./remoteRun.sh my-key.pem clientIP.txt ubuntu "sed -i 's/num.opFinisher.threads=8/num.opFinisher.threads=32/g' hedera-services/test-clients/src/main/resource/spec-default.properties "

#./remoteRun.sh my-key.pem clientIP.txt ubuntu "sed -i 's/default.payer=0.0.2/default.payer=0.0.950/g' hedera-services/test-clients/src/main/resource/spec-default.properties "

# rsync -arze "ssh -o StrictHostKeyChecking=no -i my-key.pem" ../../../test-clients/src/main/resource/StartUpAccount.txt ubuntu@18.215.152.169:hedera-services/test-clients/src/main/resource
# rsync -arze "ssh -o StrictHostKeyChecking=no -i my-key.pem" ../../../test-clients/src/main/resource/StartUpAccount.txt ubuntu@54.226.135.88:hedera-services/test-clients/src/main/resource
# rsync -arze "ssh -o StrictHostKeyChecking=no -i my-key.pem" ../../../test-clients/src/main/resource/StartUpAccount.txt ubuntu@34.230.5.247:hedera-services/test-clients/src/main/resource
# rsync -arze "ssh -o StrictHostKeyChecking=no -i my-key.pem" ../../../test-clients/src/main/resource/StartUpAccount.txt ubuntu@3.93.48.158:hedera-services/test-clients/src/main/resource

# ./upload.sh my-key.pem ubuntu clientIP.txt ../../../test-clients/src/main/resource hedera-services/test-clients/src/main/resource  "StartUpAccount.txt"

./remoteRun.sh my-key.pem clientIP.txt ubuntu "cd hedera-services/test-clients; export JAVA_HOME=/usr/local/java/jdk-12.0.1; mvn clean install"

#./remoteRun.sh my-key.pem clientIP.txt ubuntu "cd hedera-services/;  mvn clean install -DskipTests"

