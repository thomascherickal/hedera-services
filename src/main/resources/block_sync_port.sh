#!/usr/bin/env bash


# repeatedly block and unblock TCP ports used by synchronization
# used by node

set -eE

# wait a while for swirlds jar to start
sleep 20

trap ctrl_c INT

function ctrl_c() {
    echo "** Trapped CTRL-C"
    echo "recover firewall rules"
    sudo iptables --flush
    exit
}


while true; do
    echo "Block input and output sync port" >> exec.log
    sudo -n iptables -A INPUT -p tcp --dport 40124:40224 -j DROP
    sudo -n iptables -A OUTPUT -p tcp --sport 40124:40224 -j DROP
    sleep $(( ( RANDOM % 5 )  + 8 ))

    echo "Enable input and output sync port" >> exec.log
    sudo -n iptables -D INPUT -p tcp --dport 40124:40224 -j DROP
    sudo -n iptables -D OUTPUT -p tcp --sport 40124:40224 -j DROP
    sleep $(( ( RANDOM % 5 )  + 8 ))
done

