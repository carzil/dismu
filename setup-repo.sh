#!/usr/bin/sh
cd commons
mvn package
cd ..
mvn deploy:deploy-file -Durl=file://`pwd`/repo/ -Dfile=commons/target/commons-1.0-SNAPSHOT.jar -DgroupId=com.dismu -DartifactId=commons -Dpackaging=jar -Dversion=1.0-SNAPSHOT
