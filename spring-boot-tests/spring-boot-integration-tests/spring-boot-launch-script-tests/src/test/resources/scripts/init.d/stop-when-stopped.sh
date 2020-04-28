source ./test-functions.sh
source ./init.d/test-functions.sh
install_service
stop_service
echo "Status: $?"
