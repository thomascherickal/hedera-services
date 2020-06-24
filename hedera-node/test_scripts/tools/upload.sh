#!/bin/bash


# Arguments list
#  $1 private key
#  $2 user name
#  $3 public ip address file 
#  $4 source location of csv directory
#  $5 destination of test results
#  $6 file extension
#
#  Example:
#   ./upload.sh my-key.pem ubuntu clientIP.txt ~/Downloads/certs /home/ubuntu/services-hedera  "*.csv"

# Upload to a single node
#
# Arguments:
#  private key
#  user name
#   IP address of the node
#   source directory
#   Destination directory
uploadToNode()# args(ip,destination)
(
  echo "Try server $3  direcotry $4/$fileExtension"
  rsync -a -r -z -e "ssh -o StrictHostKeyChecking=no -i $1" \
  "$4/$fileExtension" \
  "$2@$3:$5/"
)

fileExtension=$6
resultDir="$5"
mkdir -p $resultDir
publicAddresses=( `cat "$3"` )

# runRetrieve $resultDir $3

for i in ${!publicAddresses[@]}; do
  uploadToNode $1 $2 "${publicAddresses[$i]}" $4 "$5" &
done
wait
