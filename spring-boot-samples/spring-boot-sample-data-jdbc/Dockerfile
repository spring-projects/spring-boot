FROM openjdk:13-jdk-alpine AS builder

CMD ["mvn", "clean", "install"]
COPY ./target/spring-boot-sample-data-jdbc-2.1.13.BUILD-SNAPSHOT.jar .

FROM openjdk:13-jdk-alpine AS run
COPY ./target/spring-boot-sample-data-jdbc-2.1.13.BUILD-SNAPSHOT.jar app.jar
CMD ["java", "-jar", "app.jar"]
