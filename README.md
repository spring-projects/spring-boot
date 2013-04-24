# Spring Bootstrap
Experimental work based on discussions at SpringOne2GX 2012. See also the 'bootstrap' branch of Spring.


## Elevator Pitch
Opinionated view of the Spring family so that new users can quickly get to the 'meat and potatoes'. Assumes no knowledge of the Java development ecosystem. Absolutely no code generation and no XML.


## Installing
You need to build from source for now, but when it's done instructions will look like this:

1) Get Java
Download and install the Java SDK from www.java.com

2) Get Spring
`curl -s try.springsource.org | bash` or use the Windows installer

3) Get to Work!
spr run yoursourcefile.groovy


## What? It's Groovy then? or like Grails? or another Roo?
There is a command line tool that uses Groovy underneath so that we can present simple snippets that can just run:

	@Controller
	class ThisWillActuallyRun {

		@RequestMapping("/")
		@ResponseBody
		String home() {
			return "Hello World!"
		}
	}

By inspecting the code for well known annotations we can `@Grab` appropriate dependencies and also dynamically add `import` statements. Groovy makes this really easy.

If you don't want to use the command line tool, and you would rather work using Java and an IDE you can. Just add a `main()` method that calls `SpringApplication` and add `@EnableAutoConfiguration`:

	import org.springframework.bootstrap.*;
	import org.springframework.context.annotation.*;

	@Configuration
	@EnableAutoConfiguration
	@ComponentScan
	public class MyApplication {

		public static void main(String[] args) throws Exception {
			SpringApplication.run(MyApplication.class, args);
		}

	}


	import org.springframework.beans.factory.annotation.*;
	import org.springframework.stereotype.*;
	import org.springframework.web.bind.annotation.*;

	@Controller
	public class SampleController {

			@RequestMapping("/")
			@ResponseBody
			String home() {
				return "Hello World!"
			}
}

## Under the hood
There are a number of disparate parts of Bootstrap. Here are the important classes:

### The Spring CLI
The 'spr' command line application compiles and runs Groovy source, adding `import` statements and `@Grab` annotations. The application can also watch files, automatically recompiling and restarting when they change.

### SpringApplication
The `SpringApplication` class provides the main entry point for a standalone Spring Application. Its sole job is to create and refresh an appropriate Spring `ApplicationContext`. Any contained beans that implements `CommandLineRunner` will be executed after the context has started. A `SpringApplication` can load beans from a number of different sources, including classes, packages (scanned) or XML files. By default a `AnnotationConfigApplicationContext` or `AnnotationConfigEmbeddedWebApplicationContext` depending on your classpath.

### EmbeddedWebApplicationContext
The `EmbeddedWebApplicationContext` will probably be part of Spring 4.0. It provides a Spring 'WebApplicationContext' that can bootstrap itself and start and embedded servlet container. Support is provided for Tomcat and Jetty.

### @EnableAutoConfigure
The `@EnableAutoConfigure` can be used on a `@Configuration` class to trigger auto-configuration of the Spring context. Auto-configuration attempts to guess what beans a user might want based on their classpath. For example, If a 'HSQLDB' is on the classpath the user probably wants an in-memory database to be defined. Auto-configuration will back away as the user starts to define their own beans.

### @Conditional
The `@Conditional` annotation will probably be part of Spring 4.0. It provides allows `@Configuration` classes to be skipped depending on conditions. Bootstrap provides `@ConditionalOnBean`, `@ConditionalOnMissingBean` and `@ConditionalOnClass` annotations are used when defining auto-configuration classes.

## Building the code
Use maven to build the source code.

	mvn clean install

## Importing into eclipse
You can use m2e or `maven eclipse:eclipse`.

Project specific settings are configured for source formatting. If you are using m2e please follow these steps to install eclipse support:

* Select `Install new software` from the `help` menu
* Click `Add...` to add a new repository
* Click the `Archive...` button
* Select `org.eclipse.m2e.maveneclipse.site-0.0.1-SNAPSHOT-site.zip` from the `eclipse` folder in this checkout
* Install "Maven Integration for the maven-eclipse-plugin"

If you prefer you can import settings manually from the `/eclipse` folder.

## Samples
The following samples are included. To run use `java -jar <archive>-full.jar`

* spring-bootstrap-simple-sample - A simple command line application
* spring-bootstrap-jetty-sample - Embedded Jetty
* spring-bootstrap-tomcat-sample - Embedded Tomcat
* spring-bootstrap-data-sample - Spring Data JPA + Hibernate + HSQLDB

