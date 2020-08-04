source ./test-functions.sh
install_service
echo 'LOG_FOLDER=log' > /test-service/spring-boot-app.conf
mkdir -p /test-service/log
useradd andy
chown andy /test-service/spring-boot-app.jar
start_service
await_app
ls -al /test-service/log/spring-boot-app.log
