# Spring Zero
Spring Zero is "Spring for Snowboarders".  If you are kewl, or just impatient, and you 
want to use Spring, then this is the place to be. Spring Zero is the code-name for a
group of related technologies, that will get you up and running with 
Spring-powered,  production-grade applications and services with absolute minimum fuss. 
It takes an opinionated view of the Spring family so that new and existing users can 
quickly get to the bits they need. Assumes limited knowledge of the Java development 
ecosystem. Absolutely no code generation and no XML (unless you really want it).

The goals are:

* Radically faster and widely accessible getting started experience for Spring
  development
* Be opinionated out of the box, but get out of the way quickly as requirements start to 
  diverge from the defaults
* Provide a range of non-functional features that are common to large classes of projects
  (e.g. embedded servers, security, metrics, health checks, externalized configuration)
* First class support for REST-ful services, modern web applications, batch jobs, and 
  enterprise integration
* Applications that adapt their behavior or configuration to their environment
* Optionally use Groovy features like DSLs and AST transformations to accelerate the 
  implementation of basic business requirements


## Installing
You need to [build from source](#building-from-source) for now, but when it's done 
instructions will look like this:

1) Get Java.  Download and install the Java SDK from [www.java.com](http://www.java.com)

2) Get Spring

	$ curl -s initializr.cfapps.io/installer | bash

   or use the [Windows installer](#installing)


## Building from source
Spring Zero can be [built with maven](http://maven.apache.org/run-maven/index.html) v3.0 
or above.

	$ mvn clean install

An `alias` can be used for the Spring Zero command line tool:

	$ alias spring="java -jar ~/.m2/repository/org/springframework/boot/spring-cli/0.5.0.BUILD-SNAPSHOT/spring-cli-0.5.0.BUILD-SNAPSHOT.jar"

_Also see [docs/CONTRIBUTING](docs/CONTRIBUTING.md) if you want to submit pull requests._  


## Quick Start Example
The Spring Zero command line tool uses Groovy underneath so that we can present simple 
snippets  that can just run, for example:

	$ cat > app.groovy
	@Controller
	class ThisWillActuallyRun {
		@RequestMapping("/")
		@ResponseBody
		String home() {
			return "Hello World!"
		}
	}
	<ctrl-d>
	$ spring run app.groovy
	$ curl localhost:8080
	Hello World!


If you don't want to use the command line tool, or you would rather work using Java and 
an IDE you can. Just add a `main()` method that calls `SpringApplication` and 
add `@EnableAutoConfiguration`:

	import org.springframework.stereotype.*;
	import org.springframework.web.bind.annotation.*;
	import org.springframework.bootstrap.context.annotation.*;
	
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
	
_NOTE: the above example assumes your build system has imported the `spring-starter-web`
maven pom._


## Spring Zero Components
There are a number of components in Zero. Here are the important ones:

### The Spring CLI
The 'spring' command line application compiles and runs Groovy source, making it super 
easy to write the absolute minimum of code to get an application running. Spring CLI 
can also watch files, automatically recompiling and restarting when they change.

*See [spring-cli/README](spring-cli/README.md).*


### Spring Bootstrap
The main library providing features that support the other parts of Spring Zero. 
Features include:

* `SpringApplication` - a class with static convenience methods that make it really easy 
  to write a standalone Spring Application. Its sole job is to create and refresh an 
  appropriate Spring `ApplicationContext`.
* Embedded web applications with a choice of container (Tomcat or Jetty for now)
* First class externalized configuration support 

_See [spring-bootstrap/README](spring-bootstrap/README.md)._

  
### Spring Autoconfigure
Spring Zero can configure large parts of common applications based on detecting the 
content of the classpath and any existing application context. A single 
`@EnableAutoConfigure` annotation triggers auto-configuration of the Spring context. 

Auto-configuration attempts to guess what beans a user might want  based on their 
classpath. For example, If a 'HSQLDB' is on the classpath the user probably wants an 
in-memory database to be defined. Auto-configuration will back away as the user starts 
to define their own beans.

_See [spring-autoconfigure/README](spring-autoconfigure/README.md)._


### Spring Actuator
Spring Actuator uses auto-configuration to decorate your application with features that 
make it instantly deployable and supportable in production.  For instance if you are 
writing a JSON web service then it will provide a server, security, logging, externalized
configuration, management endpoints, an audit abstraction, and more. If you want to 
switch off the built in features, or extend or replace them, it makes that really easy as well.

_See [spring-actuator/README](spring-actuator/README.md)._


### Spring Starters
Spring Starters are a set of convenient dependency descriptors that you can include in 
your application. You get a one-stop-shop for all the Spring and related technology 
that you need without having to hunt through sample code and copy paste loads of
dependency descriptors. For example, if you want to get started using Spring and JPA for 
database access just include one dependency in your project, and you are good to go.

_See [spring-starters/README](spring-starters/README.md)._


### Packaging
The [spring-launcher](spring-launcher/) and 
[spring-maven-packaging-plugin](spring-maven-packaging-plugin) provide a convenient way
to package you application for release. Applications can be released as a single jar
file that can simply be launched using `java -jar`.

_See [spring-launcher/README](spring-launcher/README.md) & 
[spring-package-maven-plugin/README](spring-package-maven-plugin/README.md)._


## Samples
Groovy samples for use with the command line application are available in
[spring-cli/samples](spring-cli/samples/#). To run the CLI samples type 
`spring run <sample>.groovy` from samples directory.

Java samples are available in [spring-boot-sample](spring-boot-samples/#) and should
be build with maven and run use `java -jar target/<sample>.jar`. The following java
samples are provided:

* spring-boot-sample-simple - A simple command line application
* spring-boot-sample-tomcat - Embedded Tomcat
* spring-boot-sample-jetty - Embedded Jetty
* spring-boot-sample-actuator - Simple REST service with production features
* spring-boot-sample-actuator-ui - A web UI example with production features
* spring-boot-sample-web-ui - A thymeleaf web application
* spring-sample-batch - Define and run a Batch job in a few lines of code
* spring-sample-data-jpa - Spring Data JPA + Hibernate + HSQLDB
* spring-boot-sample-integration - A spring integration application
* spring-boot-sample-profile - example showing Spring's `@profile` support
* spring-boot-sample-traditional - shows Spring Zero with more traditional WAR packaging 
  (but also executable using `java -jar`)
* spring-boot-sample-xml - Example show how Spring Zero can be mixed with trditional XML
  configuration

