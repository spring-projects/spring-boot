# Spring Boot - Samples - Web Services

This sample project demonstrates how to bootstrap and use Spring Web Services with Spring Boot.

It is a runnable implementation of the HolidayRequest sample in the Spring Web Services [reference guide](http://docs.spring.io/spring-ws/site/reference/html/tutorial.html#tutorial.implementing.endpoint).

The sample can be build with Maven (>3) and simply run from the command line.

```
$ mvn package
$ java -jar target/*.jar
```

Now pointing your browser to [http://localhost:8080/services/holidayService/holiday.wsdl] should display the generated WSDL.