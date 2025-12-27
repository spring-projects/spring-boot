FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy built JAR (adjust path if needed)
COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]

