source ./test-functions.sh
source ./init.d/test-functions.sh
install_service
status_service
echo "Status: $?"
