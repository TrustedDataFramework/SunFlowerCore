#!/bin/bash
if [ $# -lt 1 ]; then
	echo "Usage: $0 <seq_num>"
	exit 1
fi
./gradlew -Dethereumj.conf.file=src/main/resources/vrf${1}.conf runCustom
