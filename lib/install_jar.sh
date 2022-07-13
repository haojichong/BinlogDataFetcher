#!/bin/sh -
BASE="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JDBC=$BASE/jdbc
DORIS=$BASE/doris
mvn install:install-file -Dfile=$BASE/dingding/taobao-sdk-java-auto_1479188381469-20190806.jar -DgroupId=com.taobao -DartifactId=dingding -Dversion=0.1 -Dpackaging=jar
