# Spring Boot Actuator Sample
You can build this sample using Maven (>3) or Gradle (1.6).

With Maven:

```
$ mvn package
$ java -jar target/*.jar
```

Then access the app via a browser (or curl) on http://localhost:8080 (the user name is
"user" and look at the INFO log output for the password to login).

With gradle:

```
$ gradle build
$ java -jar build/libs/*.jar
```

The gradle build contains an intentionally odd configuration to exclude the security
dependencies from the executable JAR. So the app run like this behaves differently than
the one run from the Maven-built JAR file. See comments in the `build.gradle` for details.
