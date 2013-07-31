# Spring Boot
Spring Boot makes it easy to create Spring-powered, production-grade applications and 
services with absolute minimum fuss. It takes an opinionated view of the Spring platform 
so that new and existing users can quickly get to the bits they need.

You can use Spring Boot to create stand-alone Java applications that can be started using 
`java -jar` or more traditional WAR deployments. We also provide a command line tool
that runs spring scripts.

Our primary goals are:

* Provide a radically faster and widely accessible getting started experience for all
  Spring development
* Be opinionated out of the box, but get out of the way quickly as requirements start to
  diverge from the defaults
* Provide a range of non-functional features that are common to large classes of projects
  (e.g. embedded servers, security, metrics, health checks, externalized configuration)
* Absolutely no code generation and no requirement for XML configuration

## Quick Start Script Example
The Spring Zero command line tool uses [Groovy](http://groovy.codehaus.org/) underneath 
so that we can present simple Spring snippets that can 'just run', for example:

```groovy
@Controller
class ThisWillActuallyRun {
	
	@RequestMapping("/")
	@ResponseBody
	String home() {
		return "Hello World!"
	}
	
}
```

```
$ spring run app.groovy
$ curl localhost:8080
Hello World!
```

_See [below](#installing-the-cli) for command line tool installation instructions._

## Quick Start Java Example
If you don't want to use the command line tool, or you would rather work using Java and
an IDE you can. Just add a `main()` method that calls `SpringApplication` and
add `@EnableAutoConfiguration`:

```java
import org.springframework.boot.*;
import org.springframework.boot.autoconfiguration.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

@Controller
@EnableAutoConfiguration
public class SampleController {

	@RequestMapping("/")
	@ResponseBody
	String home() {
		return "Hello World!"
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleController.class, args);
	}

}
```

_NOTE: the above example assumes your build system has imported the `spring-starter-web` maven pom._

## Installing the CLI
You need [Java SDK v1.6](http://www.java.com) or higher to run the command line tool. You
should check your current Java installation before you begin:

	$ java -version
	
Complete installation instructions TBD. For now you can 
[build from source](#building-from-source).

## Building from source
Spring Boot can be [built with maven](http://maven.apache.org/run-maven/index.html) v3.0
or above.

	$ mvn clean install

You can use an `alias` for the Spring Boot command line tool:

	$ alias spring="java -jar ~/.m2/repository/org/springframework/boot/spring-cli/0.5.0.BUILD-SNAPSHOT/spring-cli-0.5.0.BUILD-SNAPSHOT.jar"

_Also see [CONTRIBUTING.md] if you wish to submit pull requests._

## Spring Boot Modules
There are a number of modules in Spring Boot. Here are the important ones:

### spring-boot
The main library providing features that support the other parts of Spring Boot,
these include:

* The `SpringApplication` class, providing static convenience methods that make it easy
  to write a stand-alone Spring Application. Its sole job is to create and refresh an
  appropriate Spring `ApplicationContext`
* Embedded web applications with a choice of container (Tomcat or Jetty for now)
* First class externalized configuration support
* Convenience `ApplicationContext` initializers, including support for sensible logging
  defaults.

_See [spring-boot/README.md](spring-boot/README.md)._


### spring-boot-autoconfigure
Spring Boot can configure large parts of common applications based on the content 
of their classpath. A single `@EnableAutoConfiguration` annotation triggers 
auto-configuration of the Spring context.

Auto-configuration attempts to deduce which beans a user might need. For example, If 
'HSQLDB' is on the classpath, and the user has not configured any database connections,
then they probably want an in-memory database to be defined. Auto-configuration will 
always back away as the user starts to define their own beans.

_See [spring-boot-autoconfigure/README.md](spring-boot-autoconfigure/README.md)._


### spring-boot-starters
Starters are a set of convenient dependency descriptors that you can include in
your application. You get a one-stop-shop for all the Spring and related technology
that you need without having to hunt through sample code and copy paste loads of
dependency descriptors. For example, if you want to get started using Spring and JPA for
database access just include the `spring-boot-starter-data-jpa` dependency in your 
project, and you are good to go.

_See [spring-boot-starters/README.md](spring-boot-starters/README.md)._


### spring-boot-cli
The Spring command line application compiles and runs Groovy source, making it super
easy to write the absolute minimum of code to get an application running. Spring CLI
can also watch files, automatically recompiling and restarting when they change.

*See [spring-boot-cli/README.md](spring-boot-cli/README.md).*


### spring-boot-ops
Ops uses auto-configuration to decorate your application with features that
make it instantly deployable and supportable in production.  For instance if you are
writing a JSON web service then it will provide a server, security, logging, externalized
configuration, management endpoints, an audit abstraction, and more. If you want to
switch off the built in features, or extend or replace them, it makes that really easy as 
well.

_See [spring-boot-ops/README.md](spring-boot-ops/README.md)._


### spring-boot-loader
Loader provides the secret sauce that allows you to build a single jar file that can be
launched using `java -jar`. Generally you will not need to use `spring-boot-loader` 
directly but instead work with the 
[Gradle](spring-boot-gradle-plugin/README.md) or 
[Maven](spring-boot-maven-plugin/README.md) plugin.

_See [spring-boot-loader/README.md](spring-boot-loader/README.md)._


## Samples
Groovy samples for use with the command line application are available in
[spring-boot-cli/samples](spring-boot-cli/samples/#). To run the CLI samples type
`spring run <sample>.groovy` from samples directory.

Java samples are available in [spring-boot-samples](spring-boot-samples/#) and should
be build with maven and run use `java -jar target/<sample>.jar`. The following java 
samples are provided:

* [spring-boot-sample-simple](spring-boot-sample-simple) - A simple command line application
* [spring-boot-sample-tomcat](spring-boot-sample-tomcat) - Embedded Tomcat
* [spring-boot-sample-jetty](spring-boot-sample-jetty) - Embedded Jetty
* [spring-boot-sample-ops](spring-boot-sample-ops) - Simple REST service with production features
* [spring-boot-sample-ops-ui](spring-boot-sample-ops-ui) - A web UI example with production features
* [spring-boot-sample-web-ui](spring-boot-sample-web-ui) - A thymeleaf web application
* [spring-boot-sample-web-static](spring-boot-sample-web-static) - A web application service static files
* [spring-sample-batch](spring-sample-batch) - Define and run a Batch job in a few lines of code
* [spring-sample-data-jpa](spring-sample-data-jpa) - Spring Data JPA + Hibernate + HSQLDB
* [spring-boot-sample-integration](spring-boot-sample-integration) - A spring integration application
* [spring-boot-sample-profile](spring-boot-sample-profile) - example showing Spring's `@profile` support
* [spring-boot-sample-traditional](spring-boot-sample-traditional) - shows more traditional WAR packaging
  (but also executable using `java -jar`)
* [spring-boot-sample-xml](spring-boot-sample-xml) - Example show how Spring Boot can be mixed with traditional 
  XML configuration (we generally recommend using Java `@Configuration` whenever possible)
