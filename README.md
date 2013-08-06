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

## Spring Boot CLI
The Spring Boot CLI is a command line tool that can be used if you want to quickly 
prototype with Spring. It allows you to run [Groovy](http://groovy.codehaus.org/) scripts, 
which means that you have a familiar Java-like syntax, without so much boilerplate code.

You don't need to use the CLI to work with Spring Boot but it's definitely the quickest
way to get a Spring application off the ground.

> **Note:** If you don't want to use the CLI,
> [jump ahead to the Java example](#quick-start-java-example).

### Installing the CLI

You need [Java SDK v1.6](http://www.java.com) or higher to run the command line tool
(there are even some issues with the `1.7.0_25` build of openjdk, so stick to earlier 
builds or use `1.6` for preference). You should check your current Java installation 
before you begin:

	$ java -version

### Manual installation
You can download the Spring CLI distribution from the Spring software repository:

* [spring-boot-cli-0.5.0.BUILD-SNAPSHOT-bin.zip](http://repo.springsource.org/milestone/org/springframework/boot/spring-boot-cli/0.5.0.M1/spring-boot-cli-0.5.0.M1-bin.zip)
* [spring-boot-cli-0.5.0.BUILD-SNAPSHOT-bin.tar.gz](http://repo.springsource.org/milestone/org/springframework/boot/spring-boot-cli/0.5.0.M1/spring-boot-cli-0.5.0.M1-bin.tar.gz)

Cutting edge [snapshot distributions](http://repo.springsource.org/snapshot/org/springframework/boot/spring-boot-cli/)
are also available.

Once downloaded, follow the
[INSTALL](spring-boot-cli/src/main/content/INSTALL.txt) instructions
from the unpacked archive. In summary: there is a `spring` script
(`spring.bat` for Windows) in a `bin/` directory in the `.zip` file,
or alternatively you can use `java -jar` with the `.jar` file (the
script helps you to be sure that the classpath is set correctly).

### OSX Homebrew installation
If you are on a Mac and using [homebrew](http://brew.sh/), all you need to do to install
the Spring Boot CLI is:

	$ brew install spring-boot-cli

Homebrew will install `spring` to `/usr/local/bin`. Now you can jump right to a
[quick start example](#quick-start-script-example).

> **Note:** If you don't see the formula, you're installation of brew might be
> out-of-date. Just execute `brew update` and try again.

### Quick start script example
Here's a really simple web application. Create a file called `app.groovy`:

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

Then run it from a shell:

```
$ spring run app.groovy
```

> **Note:** It will take some time when you first run the application as dependencies
> are downloaded, subsequent runs will be much quicker.

Open [http://localhost:8080](http://localhost:8080) in your favorite web browser and you
should see  the following output:
> Hello World!

## Spring Boot with Java
If you don't want to use the command line tool, or you would rather work using Java and
an IDE you can. Here is how you build the same example using Java.

### Quick start Maven POM
You will need to install [Apache Maven](http://maven.apache.org/) v3.0.5 or above to build
this example.

Create a `pom.xml` to import the appropriate Spring Boot starters:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<groupId>com.example</groupId>
	<artifactId>myproject</artifactId>
	<version>0.0.1-SNAPSHOT</version>

	<!-- Inherit defaults from Spring Boot -->
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>0.5.0.M1</version>
	</parent>

	<!-- Add typical dependencies for a web application -->
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
	</dependencies>

	<!-- Package as an executable JAR -->
	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>

	<!-- Allow access to Spring milestones and snapshots -->
	<!-- (you don't need this if you are using the GA release) -->
	<repositories>
		<repository>
			<id>spring-snapshots</id>
			<url>http://repo.springsource.org/snapshot</url>
			<snapshots><enabled>true</enabled></snapshots>
		</repository>
		<repository>
			<id>spring-milestones</id>
			<url>http://repo.springsource.org/milestone</url>
			<snapshots><enabled>true</enabled></snapshots>
		</repository>
	</repositories>
	<pluginRepositories>
		<pluginRepository>
			<id>spring-snapshots</id>
			<url>http://repo.springsource.org/snapshot</url>
		</pluginRepository>
		<pluginRepository>
			<id>spring-milestones</id>
			<url>http://repo.springsource.org/milestone</url>
		</pluginRepository>
	</pluginRepositories>
</project>
```

> **Note:** If you prefer [Gradle](http://www.gradle.org) as your build system, we provide
> a [plugin](spring-boot-tools/spring-boot-gradle-plugin/README.md) that can help you
> package an executable JAR.

### Quick start Java example
Here is the main class for a simple web application (just save the content to
`src/main/java/SampleController.java`):

```java
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

@Controller
@EnableAutoConfiguration
public class SampleController {

	@RequestMapping("/")
	@ResponseBody
	String home() {
		return "Hello World!";
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleController.class, args);
	}
}
```

Other than import statements, the main difference between this
example and the earlier Groovy script is the `main()` method that calls
`SpringApplication` and the `@EnableAutoConfiguration` annotation.

You can run this application by building a `jar` and executing it:

```
$ mvn package
$ java -jar target/myproject-0.0.1-SNAPSHOT.jar
```

Open [http://localhost:8080](http://localhost:8080) in your favorite web browser and you
should see  the following output:
> Hello World!

## Building Spring Boot from source
You don't need to build from source to use Spring Boot (it's in
[repo.springsource.org](http://repo.springsource.org)), but if you want to try out the
latest and greatest, Spring Boot can be
[built with maven](http://maven.apache.org/run-maven/index.html) v3.0.5 or above.

	$ mvn clean install

_Also see [CONTRIBUTING.md](CONTRIBUTING.md) if you wish to submit pull requests._

## Further Reading
There are a number of modules in Spring Boot, if you want learn more about each one
please refer to the appropriate README.md file:

> **Note:** We are currently still working on documentation for Spring Boot.

### spring-boot
The main library providing features that support the other parts of Spring Boot,
these include:

* The `SpringApplication` class, providing static convenience methods that make it easy
to write a stand-alone Spring Application. Its sole job is to create and refresh an
appropriate Spring `ApplicationContext`
* Embedded web applications with a choice of container (Tomcat or Jetty for now)
* First class externalized configuration support
* Convenience `ApplicationContext` initializers, including support for sensible logging
defaults

_See [spring-boot/README.md](spring-boot/README.md)._

### spring-boot-autoconfigure
Spring Boot can configure large parts of common applications based on the content
of their classpath. A single `@EnableAutoConfiguration` annotation triggers
auto-configuration of the Spring context.

Auto-configuration attempts to deduce which beans a user might need. For example, If
`HSQLDB` is on the classpath, and the user has not configured any database connections,
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

### spring-boot-actuator
Spring Boot Actuator provides additional auto-configuration to decorate your application
with features that make it instantly deployable and supportable in production.  For
instance if you are writing a JSON web service then it will provide a server, security,
logging, externalized configuration, management endpoints, an audit abstraction, and
more. If you want to switch off the built in features, or extend or replace them, it
makes that really easy as well.

_See [spring-boot-actuator/README.md](spring-boot-actuator/README.md)._

### spring-boot-loader
Spring Boot Loader provides the secret sauce that allows you to build a single jar file
that can be launched using `java -jar`. Generally you will not need to use
`spring-boot-loader` directly, but instead work with the
[Gradle](spring-boot-tools/spring-boot-gradle-plugin/README.md) or
[Maven](spring-boot-tools/spring-boot-maven-plugin/README.md) plugin.

_See [spring-boot-loader/README.md](spring-boot-tools/spring-boot-loader/README.md)._

## Samples
Groovy samples for use with the command line application are available in
[spring-boot-cli/samples](spring-boot-cli/samples). To run the CLI samples type
`spring run <sample>.groovy` from samples directory.

Java samples are available in [spring-boot-samples](spring-boot-samples) and should
be build with maven and run use `java -jar target/<sample>.jar`. The following java
samples are provided:

* [spring-boot-sample-simple](spring-boot-samples/spring-boot-sample-simple) -
A simple command line application
* [spring-boot-sample-tomcat](spring-boot-samples/spring-boot-sample-tomcat) -
Embedded Tomcat
* [spring-boot-sample-jetty](spring-boot-samples/spring-boot-sample-jetty) -
Embedded Jetty
* [spring-boot-sample-actuator](spring-boot-samples/spring-boot-sample-actuator) -
Simple REST service with production features
* [spring-boot-sample-actuator-ui](spring-boot-samples/spring-boot-sample-actuator-ui) -
A web UI example with production features
* [spring-boot-sample-web-ui](spring-boot-samples/spring-boot-sample-web-ui) -
A thymeleaf web application
* [spring-boot-sample-web-static](spring-boot-samples/spring-boot-sample-web-static) -
A web application service static files
* [spring-sample-batch](spring-boot-samples/spring-sample-batch) -
Define and run a Batch job in a few lines of code
* [spring-sample-data-jpa](spring-boot-samples/spring-sample-data-jpa) -
Spring Data JPA + Hibernate + HSQLDB
* [spring-boot-sample-integration](spring-boot-samples/spring-boot-sample-integration) -
A spring integration application
* [spring-boot-sample-profile](spring-boot-samples/spring-boot-sample-profile) -
example showing Spring's `@profile` support
* [spring-boot-sample-traditional](spring-boot-samples/spring-boot-sample-traditional) -
shows more traditional WAR packaging
(but also executable using `java -jar`)
* [spring-boot-sample-xml](spring-boot-samples/spring-boot-sample-xml) -
Example show how Spring Boot can be mixed with traditional XML configuration (we
generally recommend using Java `@Configuration` whenever possible)


