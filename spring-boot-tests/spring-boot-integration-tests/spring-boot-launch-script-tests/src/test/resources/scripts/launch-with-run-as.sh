source ./test-functions.sh
install_service

useradd wagner
echo 'RUN_AS=wagner' > /test-service/spring-boot-app.conf

start_service
await_app

ls -la /var/log/spring-boot-app.log
