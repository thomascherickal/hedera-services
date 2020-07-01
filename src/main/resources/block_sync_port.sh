#!/usr/bin/env bash


# repeatedly block and unblock TCP ports used by gossip

set -eE

# wait a while for swirlds jar to start
sleep 20

trap ctrl_c INT

#
# Cleanup all firewall rules if this script is terminated unexpecctedly
#
function ctrl_c() {
    echo "recover default firewall rules"
    sudo iptables --flush
    exit
}


while true; do
    echo "Block input and output gossip port" >> exec.log
    sudo -n iptables -A INPUT -p tcp --dport 40124:40224 -j DROP
    sudo -n iptables -A OUTPUT -p tcp --sport 40124:40224 -j DROP

    # sleep long enough to cause the time out of sync listner
    sleep $(( ( RANDOM % 5 )  + 8 ))

    echo "Enable input and output gossip
     port" >> exec.log
    sudo -n iptables -D INPUT -p tcp --dport 40124:40224 -j DROP
    sudo -n iptables -D OUTPUT -p tcp --sport 40124:40224 -j DROP

    # sleep long enough to sync with others to catch up some events
    sleep $(( ( RANDOM % 5 )  + 8 ))
done

