source ./test-functions.sh
install_service

chmod o+w /var/log

useradd phil
mkdir /phil-files
chown phil /phil-files

useradd andy
chown andy /test-service/spring-boot-app.jar

start_service
stop_service

su - andy -c "ln -s -f /phil-files /var/log/spring-boot-app.log"

start_service

ls -ld /phil-files
