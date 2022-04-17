source ./test-functions.sh
install_service
echo 'LOG_FOLDER=/does/not/exist' > /test-service/spring-boot-app.conf
start_service
await_app
curl -s http://127.0.0.1:8080/
