#!/bin/bash

echo "start _hugepage.sh"
uname -a
cat /proc/sys/vm/max_map_count
sudo sh -c "echo 'vm.nr_hugepages = 51200' >> /etc/sysctl.conf" 
sudo sh -c "echo '1146880' > /proc/sys/vm/max_map_count "
sudo sysctl -p
sleep 5s
cat /sys/kernel/mm/hugepages/hugepages-2048kB/nr_hugepages
cat /proc/sys/vm/max_map_count
echo "end _hugepage.sh"
