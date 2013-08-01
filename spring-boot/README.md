# Spring Boot
Spring Boot provides the central features for the other modules in the project. It is 
relatively unopinionated and it has minimal required dependencies which makes it usable 
as a stand-alone library for anyone whose tastes diverge from ours.

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
 :: Spring Boot ::   v0.0.0.BUILD.SNAPSHOT

2013-07-31 00:08:16.117  INFO 56603 --- [           main] o.s.b.s.app.SampleApplication   : Starting SampleApplication v0.1.0 on mycomputer with PID 56603 (/apps/myapp.jar started by pwebb)
2013-07-31 00:08:16.166  INFO 56603 --- [           main] ationConfigEmbeddedWebApplicationContext : Refreshing org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext@6e5a8246: startup date [Wed Jul 31 00:08:16 PDT 2013]; root of context hierarchy
```

By default `INFO` logging messages will shown, including some relevant startup details
such as the user that launched the application.

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

Note that the constructor arguments passed to `SpringApplication` are configuration 
sources for spring beans. In most cases these will be references to `@Configuration`
classes, but they could also be references to XML configuration or to packages that
should be scanned.

See the `SpringApplication` Javadoc for a complete list of the configuration options  

### Accessing command line properties
By default `SpringApplication` will expose any command line arguments as Spring 
Properties. This allows you to easily access arguments by injecting them as `@Values`

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

You can also use the special `--spring.profiles.active` argument to enable specific
Spring profiles from the command line.

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
the `org.springframework.core.annotation.Order` annotation if several `CommandLineRunner`
beans are defined that must be called in a specific order.

### Application Exit
Each `SpringApplication` will register a shutdown hook with the JVM to ensure that the
`ApplicationContext` is closed gracefully on exit. All the standard Spring lifecycle
callbacks (such as the `DisposableBean` interface, or the `@PreDestroy` annotation)
can be used.

In addition, beans may implement the `org.springframework.boot.ExitCodeGenerator`
interface if they with to return a specific exit code when the application ends.

## Embedded Servlet Container Support
Spring Boot introduces a new type of Spring `ApplicationContext` that can be used to
start an embedded servlet container. The `EmbeddedWebApplicationContext`  is a special 
type of `WebApplicationContext` that starts the container by searching for a single 
`EmbeddedServletContainerFactory` bean contained within itself. We provide 
`TomcatEmbeddedServletContainerFactory` and `JettyEmbeddedServletContainerFactory`
 implementations for running embedded Tomcat or Jetty. 

One advantage of using a Spring bean to define the embedded container is that you can use   
all the standard Spring concepts. For example, it becomes trivial to define a Tomcat
server that sets its port from an injected `@Value`:

```java
@Configuration
public class MyConfiguration {
	
	@Value("${tomcatport:8080}")
	private String port; 

	@Bean
	public EmbeddedServletContainerFactory servletContainer() {
		return new TomcatEmbeddedServletContainerFactory(this.port);
	}

}
```

### Customizing Servlet Containers
Both the Tomcat and Jetty factories extend from the base 
`AbstractEmbeddedServletContainerFactory` class. This provides a uniform way
to configure your container regardless of which implementation you actually
choose.

Settings that you traditionally configure in a `web.xml` or via an implementation
specific configuration file can now be performed programmatically. For example:

```java
@Bean
public EmbeddedServletContainerFactory servletContainer() {
	TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
	factory.setPort(9000);
	factory.setSessionTimeout(10, TimeUnit.MINUTES);
	factory.addaddErrorPages(new ErrorPage(HttpStatus.404, "/notfound.html");
	return factory;
}
``` 

In addition, you can also add `ServletContextInitializer` implementations which allow
you to customize the `javax.servlet.ServletContext` in the same way as any Servlet 3
environment.

### Servlets and Filters
Servlets and Filters can be defined directly as beans with the 
`EmbeddedWebApplicationContext`. By default, if the context contains only a single 
Servlet it will be mapped to '/'. In the case of multiple Servlets beans the bean name 
will be used as a path prefix. Filters will map to '/*'.

If convention based mapping is not flexible enough you can use the 
`ServletRegistrationBean` and `FilterRegistrationBean` classes for complete control. You
can also register items directly if your bean implements the `ServletContextInitializer`
interface.

## External Configuration

FIXME include loadcations that SpringApplication hits
fact you can conf SpringApplication
and propery bindings

## ApplicationContextInitializers

