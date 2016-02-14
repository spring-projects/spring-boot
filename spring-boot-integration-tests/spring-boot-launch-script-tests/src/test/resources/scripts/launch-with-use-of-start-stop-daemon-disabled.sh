source ./test-functions.sh
chmod -x $(type -p start-stop-daemon)
echo 'USE_START_STOP_DAEMON=false' > /spring-boot-app.conf
install_service
start_service
await_app
curl -s http://127.0.0.1:8080/
