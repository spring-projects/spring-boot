source ./test-functions.sh
install_service

useradd phil
mkdir /phil-files
chown phil /phil-files

useradd andy
chown andy /test-service/spring-boot-app.jar

start_service
stop_service

su - andy -c "ln -s /phil-files /var/run/spring-boot-app/spring-boot-app.pid"

start_service

ls -ld /phil-files
