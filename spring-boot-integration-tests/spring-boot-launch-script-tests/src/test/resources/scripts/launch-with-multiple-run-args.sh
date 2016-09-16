source ./test-functions.sh
echo 'RUN_ARGS="--server.port=8081 --server.context-path=/test"' > /spring-boot-app.conf
install_service
start_service
await_app http://127.0.0.1:8081/test/
curl -s http://127.0.0.1:8081/test/
