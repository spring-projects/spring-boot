source ./test-functions.sh
install_service
start_service
pid=$(cat /var/run/spring-boot-app/spring-boot-app.pid)
echo "PID: $pid"
force_stop_service
status_service
echo "Status: $?"