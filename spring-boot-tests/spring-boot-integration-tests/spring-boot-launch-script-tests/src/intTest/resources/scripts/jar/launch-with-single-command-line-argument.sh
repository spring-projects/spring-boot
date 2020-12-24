source ./test-functions.sh
launch_jar --server.port=8081
await_app http://127.0.0.1:8081/
curl -s http://127.0.0.1:8081/
