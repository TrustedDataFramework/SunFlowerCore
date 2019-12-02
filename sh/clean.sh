#!/bin/bash

date

FULL_PATH=`readlink -f $0`
BASE_DIR=`dirname $FULL_PATH`

cd $BASE_DIR/..
pwd

rm -rf consortium/database/

