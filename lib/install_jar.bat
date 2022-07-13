@echo off
call mvn install:install-file -Dfile=%cd%\dingding\taobao-sdk-java-auto_1479188381469-20190806.jar -DgroupId=com.taobao -DartifactId=dingding -Dversion=0.1 -Dpackaging=jar
