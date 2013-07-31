# Spring Boot
Spring Boot provides the central features for the other modules in the project. It is 
relatively unopinionated and it has minimal dependencies which makes it usable as a
stand-alone library for anyone whose tastes diverge from ours.

## SpringApplication
The `SpringApplication` class provides a convenient way to bootstrap a Spring application
that will be started from a `main()` method. In many situations you can just delegate
to the static `SpringApplication.run` method:

```java
public static void main(String[] args) {
	SpringApplication.run(SpringConfiguration.class, args);
}
```

When you application starts you should see something similar to the following:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 Spring Boot (v0.5.0.BUILD-SNAPSHOT)

2013-07-31 00:08:16.117  INFO 56603 --- [           main] o.s.b.s.app.SampleApplication   : Starting SampleApplication v0.1.0 on mycomputer with PID 56603 (/apps/myapp.jar started by pwebb)
2013-07-31 00:08:16.166  INFO 56603 --- [           main] ationConfigEmbeddedWebApplicationContext : Refreshing org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext@6e5a8246: startup date [Wed Jul 31 00:08:16 PDT 2013]; root of context hierarchy
```

By default `INFO` logging messages will shown, including some relevant startup information
such as the user that started the application.

### Customizing SpringApplication
If the SpringApplication defaults aren't to your taste you can instead create a local
instance and customize it. For example, to turn off the banner you would write:

```java
public static void main(String[] args) {
	SpringApplication app = new SpringApplication(SpringConfiguration.class);
	app.setShowBanner(false);
	app.run(args);
}
```

See the `SpringApplication` Javadoc for a complete list of the configuration options  

### Accessing command line properties
By default `SpringApplication` will expose any command line arguments as Spring 
Properties. This allows you to easily access arguments using by injecting them
as `@Values`

```java
import org.springframework.stereotype.*
import org.springframework.beans.factory.annotation.*

@Component
public class MyBean {
	
	@Value("${name}")
	private String name; 
	// Running 'java -jar myapp.jar --name=Spring' will set this to "Spring"

	// ...	
}
```

### CommandLineRunner beans
If you wan't access to the raw command line argument, or you need to run some specific
code once the `SpringApplication` has started you can implement the `CommandLineRunner`
interface. The `run(String... args)` method will be called on all spring beans 
implementing the interface.

```java
import org.springframework.boot.*
import org.springframework.stereotype.*

@Component
public class MyBean implements CommandLineRunner {

	public void run(String... args) {
		// Do something...
	}
	
}
```

You can additionally implement the `org.springframework.core.Ordered` interface or use
the `org.springframework.core.annotation.Order` annotation if serveral `CommandLineRunner`
beans are defined that must be called in a specific order.

### Application Exit



## Embedded Servlet Container Support

## External Configuration

## Conditionals

## ApplicationContextInitializers

