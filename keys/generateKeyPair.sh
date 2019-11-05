#!/usr/bin/env bash
cd "`dirname "$0"`"

openssl genrsa -out my-key.pem 2048
openssl rsa -in my-key.pem -pubout > my-key.pub