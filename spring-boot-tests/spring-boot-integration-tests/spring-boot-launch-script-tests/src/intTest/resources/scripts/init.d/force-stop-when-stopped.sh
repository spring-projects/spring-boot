source ./test-functions.sh
source ./init.d/test-functions.sh
install_service
force_stop_service
echo "Status: $?"
