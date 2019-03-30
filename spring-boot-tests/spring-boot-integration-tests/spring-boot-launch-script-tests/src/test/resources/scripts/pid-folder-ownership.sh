source ./test-functions.sh
install_service

chmod o+w /var/run

useradd phil
mkdir /phil-files
chown phil /phil-files

useradd andy
chown andy /test-service/spring-boot-app.jar

su - andy -c "ln -s -f /phil-files /var/run/spring-boot-app"

start_service

ls -ld /phil-files
