source ./test-functions.sh
install_service
start_service
pid=$(cat /var/run/spring-boot-app/spring-boot-app.pid)
echo "PID: $pid"
kill -9 $pid
status_service
echo "Status: $?"
