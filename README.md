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

## Installing the CLI

The Spring Boot command line tool uses
[Groovy](http://groovy.codehaus.org/) underneath so that we can
present simple Spring snippets that can 'just run'.  You don't need
the CLI to get started (see the
[Java example](#quick-start-java-example) below), but it's the
quickest way to get a Spring application off the ground.  You need
[Java SDK v1.6](http://www.java.com) or higher to run the command line
tool (there are even some issues with the `1.7.0_25` build of openjdk,
so stick to earlier builds or use `1.6` for preference). You should
check your current Java installation before you begin:

	$ java -version

### MacOS with Brew

If you are on a Mac and using [homebrew](http://brew.sh/), all you need do to install the Spring Boot CLI is:

    $ brew install spring-boot-cli
    
It will install `/usr/local/bin/spring`. Now you can jump right to a [quick start example](#quick-start-groovy-example).

> **Note:** If you don't see the formula, you're installation of brew might be out-of-date. Just execute `brew update` and try again

### Cross Platform `java - jar`
An alternative way to install Spring Boot CLI is to downloaded it from our Maven repository, and then you can use a shell `alias`:

    $ wget http://maven.springframework.org/milestone/org/springframework/boot/spring-boot-cli/0.5.0.M1/spring-boot-cli-0.5.0.M1.jar
    $ alias spring="java -jar `pwd`/spring-boot-cli-0.5.0.M1.jar"

If you don't have `wget` installed on your system you might have
`curl` (with `-o` for setting the output filename). Windows users
will need [cygwin](http://www.cygwin.org) to use the `alias` command,
but they can run `java -jar` directly and that will work.

Complete installation including a downloadable `.zip` with a shell
script TBD.

<a name="quick-start-groovy-example"></a>
## Quick Start Script Example
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
$ spring run app.groovy --verbose
$ curl localhost:8080
Hello World!
```

It might take a few minutes the first time you do this while some
dependencies are downloaded (which is why we added the `--verbose`
option - you can remove that if you prefer). If you are a maven user
and have a fully loaded local cache with all the required dependencies
you will find it is much faster.

<span id="quick-start-java-example"/>
## Quick Start Java Example
If you don't want to use the command line tool, or you would rather work using Java and
an IDE you can. Create a `pom.xml` (or the equivalent with your favourite build system):

`pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <modelVersion>4.0.0</modelVersion>
    <artifactId>myproject</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>0.5.0.M1</version>
    </parent>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>${project.groupId}</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
    <!-- TODO: remove once Spring Boot is in Maven Central -->
    <repositories>
        <repository>
            <id>spring-milestone</id>
            <url>http://repo.springsource.org/milestone</url>
        </repository>
    </repositories>
    <pluginRepositories>
        <pluginRepository>
            <id>spring-milestone</id>
            <url>http://repo.springsource.org/milestone</url>
        </pluginRepository>
    </pluginRepositories>
</project>
```

Then just add a class in `src/main/java` with a `main()` method that
calls `SpringApplication` and add `@EnableAutoConfiguration`, e.g:

`src/main/java/SampleController.java`

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

You can run this application by building a `jar` and executing it:

```
$ mvn package
$ java -jar target/myproject-0.0.1-SNAPSHOT.jar
... Spring starting up ...
```

and in anonther terminal:

```
$ curl localhost:8080
Hello World!
```

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
`spring-boot-loader`  directly but instead work with the 
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

## Building Spring Boot from source You don't need to build from
source to use Spring Boot (it's in the Maven repositories), but if you
want to try out the latest and greatest, Spring Boot can be
[built with maven](http://maven.apache.org/run-maven/index.html) v3.0
or above.

	$ mvn clean install

_Also see [CONTRIBUTING.md](CONTRIBUTING.md) if you wish to submit pull requests._

