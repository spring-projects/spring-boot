source ./test-functions.sh
mkdir ./pid
install_service
echo 'LOG_FOLDER=log' > /test-service/spring-boot-app.conf
mkdir -p /test-service/log
start_service
await_app
[[ -s /test-service/log/spring-boot-app.log ]] && echo "Log written"