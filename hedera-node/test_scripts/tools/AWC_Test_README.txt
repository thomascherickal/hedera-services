
Purpose
=======

Launch AWS instances to run server nodes and client side test software

Steps
=====

0) Build HGCApp locally

    cd services-hedera
    rm -rf ~/.m2;  cd build; make all


1) Preparation for client nodes

Launch AWS instances for client side, modif _Client.sh as needed, change region, number of instances, etc

    ./startAwsInstancesAndRunTests.sh _Client.sh 

Copy IP adddress from console to a text file named clientIP.txt

This is launch multiple AWS instances, does not upload any jar files to it, does not start any java process on it.
Use later steps to build appropriate client software on those AWS instances

Once seeing console log "You can stop current AWS test with Ctrl-C",
that means instances are ready 


Command all client node to checkout correct git brnach 

    ./remoteRun.sh my-key.pem clientIP.txt ubuntu "cd services-hedera/hapiClient ; git checkout -b client-manager-thread origin/client-manager-thread ; git pull  "

Command all client node to build software

    ./remoteRun.sh my-key.pem clientIP.txt ubuntu "cd services-hedera/hapiClient ; cd ../build; make all ; cd -; mvn clean install  "


2) Preparation for server nodes

Launch AWS instances for server side, modif _Server.sh as needed, change region, number of instances, etc

    ./startAwsInstancesAndRunTests.sh _Server.sh 

Copy IP adddress from console to a text file named serverIP.txt

Once seeing console log "You can stop current AWS test with Ctrl-C",
that means instances are ready 


Check swirlds.log to make sure HGCApp are running corretly 


    ./remoteRun.sh my-key.pem serverIP.txt ubuntu "cat remoteExperiment/output/swirlds.log"

    ./remoteRun.sh my-key.pem serverIP.txt ubuntu "cat remoteExperiment/output/hgcaa.log"


3) Launch client side test 

First build a text file cmdFile.txt,
each line is one remote command will be launched on client 

Example content of cmdFile.txt

cd services-hedera/hapiClient; nohup mvn exec:java -Dexec.mainClass=com.opencrowd.client.core.ClientManager -Dexec.args=' ContractBigArray 1 SERVER_IP 50211 3 true  30000000 800 6.5 true ' -Dexec.cleanupDaemonThreads=false > test.log 2>&1
cd services-hedera/hapiClient; nohup mvn exec:java -Dexec.mainClass=com.opencrowd.client.core.ClientManager -Dexec.args=' ContractBigArray 1 SERVER_IP 50211 4 true  30000000 800 6.5 true ' -Dexec.cleanupDaemonThreads=false > test.log 2>&1
cd services-hedera/hapiClient; nohup mvn exec:java -Dexec.mainClass=com.opencrowd.client.core.ClientManager -Dexec.args=' ContractBigArray 1 SERVER_IP 50211 5 true  30000000 800 6.5 true ' -Dexec.cleanupDaemonThreads=false > test.log 2>&1
cd services-hedera/hapiClient; nohup mvn exec:java -Dexec.mainClass=com.opencrowd.client.core.ClientManager -Dexec.args=' ContractBigArray 1 SERVER_IP 50211 6 true  30000000 800 6.5 true ' -Dexec.cleanupDaemonThreads=false > test.log 2>&1

If there are 4 client, then the cmdFile.txt should have 4 lines followed by a empty line
( a bug in script has not been fixed yet)

Then launch from testing directory

    ./remoteRun.sh my-key.pem clientIP.txt ubuntu cmdFile.txt serverIP.txt


TIPS
====

You can use ./remoteRun.sh to command a list of remote server run the same command or run different command
To kill all java process in remote client instances
    ./remoteRun.sh my-key.pem clientIP.txt ubuntu "pkill java"

To stop startAwsInstancesAndRunTests.sh
just type Ctrl-C and then select No


4) Retrieve CSV during the test

./retrieve.sh  my-key.pem ubuntu serverIP.txt remoteExperiment results/staging/ "*.csv"

