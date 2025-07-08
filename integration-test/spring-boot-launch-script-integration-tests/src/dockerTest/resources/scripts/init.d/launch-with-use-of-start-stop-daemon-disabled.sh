source ./test-functions.sh
chmod -x $(type -p start-stop-daemon)
install_service
echo 'USE_START_STOP_DAEMON=false' > /test-service/spring-boot-app.conf
start_service
await_app
curl -s http://127.0.0.1:8080/
