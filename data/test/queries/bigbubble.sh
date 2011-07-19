#!/bin/sh
if [ $# -ne 1 ]
then
   echo "Usage: bubble.sh <URL>"
   echo "Example: #/bigbubble.sh komani.ath.cx:8081"
   exit 1
fi
./curl.sh $1 bigbubblesort.json bsort FooUser FooPassword                                    