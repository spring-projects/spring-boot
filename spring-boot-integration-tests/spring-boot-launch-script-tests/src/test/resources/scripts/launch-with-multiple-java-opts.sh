source ./test-functions.sh
echo 'JAVA_OPTS="-Dserver.port=8081 -Dserver.context-path=/test"' > /spring-boot-app.conf
install_service
start_service
await_app http://127.0.0.1:8081/test/
curl -s http://127.0.0.1:8081/test/
