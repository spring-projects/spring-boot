source ./test-functions.sh
install_service
start_service
echo "PID1: $(cat /var/run/spring-boot-app/spring-boot-app.pid)"
restart_service
echo "Status: $?"
echo "PID2: $(cat /var/run/spring-boot-app/spring-boot-app.pid)"
