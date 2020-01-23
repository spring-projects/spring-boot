ARG PASS
ARG USER

FROM openjdk:13-jdk-alpine AS build

CMD ["clean", "-f", "pom.xml"]
CMD ["install", "-f", "pom.xml"]
RUN echo "User: $USER"
RUN echo "Pass: $PASS"

FROM openjdk:13-jdk-alpine AS run
COPY ./target/spring-boot-sample-data-jdbc-2.1.13.BUILD-SNAPSHOT.jar ./app.jar
CMD ["java", "-jar", "app.jar"]
