if [ -z "$1" ]; then
	echo "Specify the new version"
	exit 1
fi
cd ../../spring-boot-dependencies
mvn versions:set -DgenerateBackupPoms=false -DnewVersion=$1
cd ..
mvn versions:set -DgenerateBackupPoms=false -DnewVersion=$1
cd buildhelper/all
mvn versions:set -DgenerateBackupPoms=false -DnewVersion=$1
mvn -N versions:update-child-modules -DgenerateBackupPoms=false
cd ../../spring-boot-starters
mvn -N validate
cd ../buildhelper/all
