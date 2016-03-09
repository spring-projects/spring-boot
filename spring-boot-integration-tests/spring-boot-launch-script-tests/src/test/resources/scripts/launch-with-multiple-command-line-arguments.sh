source ./test-functions.sh
install_service
start_service --server.port=8081 --server.context-path=/test
await_app http://127.0.0.1:8081/test/
curl -s http://127.0.0.1:8081/test/
