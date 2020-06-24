

0)  How to launch node servers on AWS

make sure you have your AWS private key "my-key.pem" under the same directory

run

    ./startAwsInstancesAndRunTests.sh _Server.sh ./
or 
    launch_aws_hgcap.sh 

_Server.sh is the config file where you can select how many instances, run how long
A quick introduction of it:

How many instance

    numberOfInstancesPerRegion=( 4  )
Instance type

    awsInstanceType="m4.10xlarge"

Test dudration

    experimentDuration=1200

Overridde default settings.txt

    additionalSettings=(
    "numConnections, 1000"
    "throttle7, 1"
    ...

    
AMI image version 

    awsAmi="ATF-U18.04-OJDK12.0.1-PSQL10.9-BADGERIZE-V8"


The script ./startAwsInstancesAndRunTests.sh
 will generate a test directory named like "test 2019-07-08 14-00-21 -- 4 mem, 1 region file creation/"
where you can find publicAddress.txt
which includ public ip address of node 3, 4, 5, 6, etc.

Using these ip address in client command line parameter
