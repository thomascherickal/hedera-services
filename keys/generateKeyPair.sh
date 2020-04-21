#!/usr/bin/env bash
cd "`dirname "$0"`"

ssh-keygen -t rsa -f ./my-key -q -N ""
mv my-key my-key.pem