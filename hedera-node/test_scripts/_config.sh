####################################
#
# SSH config
#
####################################

sshUsername="ec2-user"
pubfile=$(find *.pub -print -quit 2>/dev/null)
pemfile=$(find /home/*.pem -print -quit 2>/dev/null) 
if [ $? -ne 0 ]
then
  # this is needed because of Windows
  pemfile=$(find *.pem -print -quit 2>/dev/null)
fi

####################################
#
# Automatic loading of variables
#
####################################

privateAddresses=( `cat privateAddresses.txt 2>/dev/null` )
publicAddresses=( `cat publicAddresses.txt 2>/dev/null` )
localIps=`/sbin/ifconfig | awk '/inet addr/{print substr($2,6)}'` #/sbin is not in the path when running a remote command
if [ -n "$pemfile" ]; then
  chmod 600 $pemfile
fi
if [[ `pwd` == *"remoteExperiment"* ]]; then
  pathToRemoteExperiment="../remoteExperiment"
else
  if [[ "$testRunningOn" == "local" ]]; then
    pathToRemoteExperiment=".."
  else
    pathToRemoteExperiment="remoteExperiment"
  fi
fi

####################################
#
# AWS related
#
####################################

numberOfInstancesPerAwsRequest=100


####################################
#
# Additional JVM Options
#
####################################

# concurrent gc 
EXTRA_JVM_OPTS=" -Xmx120g -Xms40g -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:ConcGCThreads=24   -XX:+UseLargePages -XX:ZMarkStackSpaceLimit=16g  -Xlog:gc*:file=myapp-gc.log  -Dcom.sun.management.jmxremote.port=3333 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"

#EXTRA_JVM_OPTS="-agentpath:/home/ubuntu/jprofiler11/bin/linux-x64/libjprofilerti.so=port=8849,offline,id=107,config=/home/ubuntu/remoteExperiment/jprofiler_config.xml   -Xmx120g -Xms40g -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:ConcGCThreads=24  -XX:+UseLargePages  -Xlog:gc*:file=myapp-gc.log  -Dcom.sun.management.jmxremote.port=3333 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"


#EXTRA_JVM_OPTS="-Xmx100g -Xms40g   -Xlog:gc*:file=myapp-gc.log  -Dcom.sun.management.jmxremote.port=3333 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"


####################################
#
# Auto-detect Date Command
#
####################################
export DATE="/usr/bin/env date"

if [[ "$OSTYPE" == "darwin"* ]]; then
    export DATE="/usr/bin/env gdate"
fi