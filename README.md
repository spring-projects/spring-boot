# Spring Bootstrap

Spring Bootstrap is "Spring for Snowboarders".  If you are kewl, or
just impatient, and you want to use Spring, then this is the place to
be. Spring Bootstrap is a toolkit and runtime platform that will get
you up and running with Spring-powered, production-grade applications
and services with absolute minimum fuss. It takes an opinionated view
of the Spring family so that new and existing users can quickly get to
the bits they need. Assumes no knowledge of the Java development
ecosystem. Absolutely no code generation and no XML (unless you really
want it).

The goals are:

* Radically faster and widely accessible getting started experience
  for Spring development
* Be opinionated out of the box, but get out of the way quickly as
  requirements start to diverge from the defaults
* Provide a range of non-functional features that are common to large
  classes of projects (e.g. embedded servers, security, metrics,
  health checks, externalized configuration)
* First class support for REST-ful services, modern web applications,
  batch jobs, and enterprise integration
* Applications that adapt their behaviour or configuration to their
  environment
* Optionally use Groovy features like DSLs and AST transformations to
  accelerate the implementation of basic business requirements
  
## Installing
You need to build from source for now, but when it's done instructions will look like this:

1) Get Java.  Download and install the Java SDK from www.java.com

2) Get Spring

     $ curl -s try.spring.io | bash
     
   or use the Windows installer

3) Get to Work!

    $ cat > app.groovy
	@Controller
	class ThisWillActuallyRun {
		@RequestMapping("/")
		@ResponseBody
		String home() {
			return "Hello World!"
		}
	}
    $ spring run app.groovy
    $ curl localhost:8080
    Hello World!


## What? It's Groovy then? or like Grails? or another Roo?

There is a command line tool that uses Groovy underneath so that we
can present simple snippets that can just run just like the slimline
`app.groovy` example above.  Groovy makes this really easy.

If you don't want to use the command line tool, or you would rather
work using Java and an IDE you can. Just add a `main()` method that
calls `SpringApplication` and add `@EnableAutoConfiguration`:


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

## Spring Bootstrap Themes

There are a number of themes in Bootstrap. Here are the important
ones:

### The Spring CLI

The 'spring' command line application compiles and runs Groovy source,
making it super easy to write the absolute minimum of code to get an
application running. Spring CLI can also watch files, automatically
recompiling and restarting when they change.

### Bootstrap Core

The main library providing features that support the other parts of
Spring Bootstrap.  Features include:

* `SpringApplication` - a class with static convenience methods that
  make it really easy to write a standalone Spring Application. Its
  sole job is to create and refresh an appropriate Spring
  `ApplicationContext`.
* Embedded web applications with a choice of container (Tomcat or
  Jetty for now)
* `@EnableAutoConfigure` is an annotation that triggers
  auto-configuration of the Spring context. Auto-configuration
  attempts to guess what beans a user might want based on their
  classpath. For example, If a 'HSQLDB' is on the classpath the user
  probably wants an in-memory database to be
  defined. Auto-configuration will back away as the user starts to
  define their own beans.
* `@Conditional` is an annotation in Spring 4.0 that allows you to
  control which parts of an application are used at runtime. Spring
  Bootstrap provides some concrete implementations of conditional
  configuration, e.g. `@ConditionalOnBean`,
  `@ConditionalOnMissingBean` and `@ConditionalOnClass`.

### Spring Bootstrap Actuator

Spring Bootstrap Actuator uses auto-configuration features to decorate
your application with features that make it instantly deployable and
supportable in production.  For instance if you are writing a JSON web
service then it will provide a server, security, logging, externalized
configuration, management endpoints, an audit abstraction, and more.
If you want to switch off the built in features, or extend or replace
them, it makes that really easy as well.

### Spring Bootstrap Starters

Spring Bootstrap Starters are a set of convenient dependency
descriptors that you can include in your application.  You get a
one-stop-shop for all the Spring and related technology that you need
without having to hunt through sample code and copy paste loads of
dependency descriptors. For example, if you want to get started using
Spring and JPA for database access just include one dependency in your
project, and you are good to go.

## Building the code
Use maven to build the source code.

	$ mvn clean install

## Importing into eclipse
You can use m2e or `maven eclipse:eclipse`.

Project specific settings are configured for source formatting. If you
are using m2e you can follow these steps to install eclipse support
for formatting:

* Select `Install new software` from the `help` menu
* Click `Add...` to add a new repository
* Click the `Archive...` button
* Select `org.eclipse.m2e.maveneclipse.site-0.0.1-SNAPSHOT-site.zip`
  from the `eclipse` folder in this checkout
* Install "Maven Integration for the maven-eclipse-plugin"

Or if you prefer you can import settings manually from the `/eclipse` folder.

## Samples
The following samples are included. To run use `java -jar target/<archive>.jar`

* spring-bootstrap-simple-sample - A simple command line application
* spring-bootstrap-jetty-sample - Embedded Jetty
* spring-bootstrap-tomcat-sample - Embedded Tomcat
* spring-bootstrap-service-sample - Simple REST service with production features
* spring-batch-sample - Define and run a Batch job in a few lines of code
* spring-bootstrap-data-sample - Spring Data JPA + Hibernate + HSQLDB

