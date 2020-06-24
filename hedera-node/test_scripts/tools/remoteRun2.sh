#!/bin/bash


# launch command on clinet through SSH
#   make it easy to launch same or difference command on multiple remote machine
#
#   each remote machine may run the same command or different command
#
# Arguments list
#   $1 file name of AWS private key
#   $2 file name that holds IP addresses of remote machiens, each line is an IP address
#   $3 username used for SSH
#   $4 command to run or the file of command file if each client run different command
#      if using command file the last command must end with a new line,
#       for example, four line command must followed by an empty new line
#   $5  file name of server ip addresses
#         this is used to replace SERVER_IP field in command file
#
#  Example 
#    ./remoteRun.sh my-key.pem clientIP.txt ubuntu cmdFile.txt serverIP.txt
#
#    ./remoteRun.sh my-key.pem clientIP.txt ubuntu "cd services-hedera/hapiClient; git pull"
#

pemfile=$1
clientIPAddress=( `cat "$2"` )
sshUsername=$3
cmd="$4"


# check if the parameter is an existing file or not
if [ -f "$cmd" ]
then
    echo "----------Client run different commands---------"

    # load command file to array vairables
    IFS=$'\n' cmdList=( $(xargs -n20 <$cmd) )
    
    # load ip address 
    serverIP=( `cat "$5"` )

    # load command list to array
    cmdList=()
    while IFS= read -r line; do
        cmdList+=("$line")
    done < $cmd
    
    for i in ${!clientIPAddress[@]}; do
        cmd="${cmdList[$i]}"
        
        # replace SERVER_IP with ip address read from server ip file
        cmdWithIP="${cmd/SERVER_IP/${serverIP[$i]}}"
        
        echo " cmd = $cmdWithIP"
        ssh -t -t -o StrictHostKeyChecking=no -i $pemfile "$sshUsername@${clientIPAddress[$i]}" "$cmdWithIP" &
    done
    
else
    echo "-----------All client run same command---------"
    for i in ${!clientIPAddress[@]}; do
        ssh -t -t -o StrictHostKeyChecking=no -i $pemfile "$sshUsername@${clientIPAddress[$i]}" "$cmd" 
        echo "----------------------------------"
    done
    
fi
wait