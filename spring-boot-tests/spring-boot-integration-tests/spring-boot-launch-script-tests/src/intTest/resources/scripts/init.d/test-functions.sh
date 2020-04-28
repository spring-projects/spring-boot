source ../test-functions.sh

install_service() {
  mkdir /test-service
  mv /app.jar /test-service/spring-boot-app.jar
  chmod +x /test-service/spring-boot-app.jar
  ln -s /test-service/spring-boot-app.jar /etc/init.d/spring-boot-app
}

install_double_link_service() {
  mkdir /test-service
  mv /app.jar /test-service/
  chmod +x /test-service/app.jar
  ln -s /test-service/app.jar /test-service/spring-boot-app.jar
  ln -s /test-service/spring-boot-app.jar /etc/init.d/spring-boot-app
}

start_service() {
  service spring-boot-app start $@
}

restart_service() {
  service spring-boot-app restart
}

status_service() {
  service spring-boot-app status
}

stop_service() {
  service spring-boot-app stop
}

force_stop_service() {
  service spring-boot-app force-stop
}