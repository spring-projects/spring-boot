# Spring Boot - Core
This module provides the core features for the other modules in the project. It is
relatively unopinionated and it has minimal required dependencies which makes it usable
as a stand-alone library for anyone whose tastes diverge from ours.

## SpringApplication
The `SpringApplication` class provides a convenient way to bootstrap a Spring application
that will be started from a `main()` method. In many situations you can just delegate
to the static `SpringApplication.run` method:

```java
public static void main(String[] args) {
	SpringApplication.run(MySpringConfiguration.class, args);
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
	SpringApplication app = new SpringApplication(MySpringConfiguration.class);
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
By default `SpringApplication` will convert any command line option arguments (starting
with '--', e.g. `--server.port=9000`) to a `PropertySource` and add it to the Spring
`Environment` with highest priority (taking precedence and overriding values  from other
sources). Properties in the `Environment` (including System properties and OS environment
variables) can always be injected into Spring components using `@Value` with
 placeholders, e.g.

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
If you want access to the raw command line argument, or you need to run some specific
code once the `SpringApplication` has started you can implement the `CommandLineRunner`
interface. The `run(String... args)` method will be called on all spring beans
implementing this interface.

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
interface if they wish to return a specific exit code when the application ends.

### Externalized Configuration
A `SpringApplication` will load properties from `application.properties` in the root of
your classpath and  add them to the Spring `Environment`. The actual search path for the
files is:

1. classpath root
2. current directory
3. classpath `/config` package
4. `/config` subdir of the current directory.

The list is ordered by decreasing precedence (so properties can be overridden by others
with the same name defined in later locations). In addition, profile specific properties
can also be defined using the naming convention `application-{profile}.properties`
(properties from these files override the default ones).

The values in `application.properties` are filtered through the existing `Environment`
when they are used so you can refer back to previously defined values (e.g. from System
properties).

```
app.name: MyApp
app.description: ${app.name} is a Spring Boot application
```

If you don't like `application.properties` as the configuration file name you can
switch to another by specifying `spring.config.name` environment property. You can also
refer to an explicit location using the `spring.config.location` environment property.

    $ java -jar myproject.jar --spring.config.name=myproject


> **Note:** You can also use '.yml' files as an alternative to '.properties' (see
> [below](#using-yaml-instead-of-properties))_

### Setting the Default Spring Profile
Spring Profiles are a way to segregate parts of the application configuration and make it
only available in certain environments.  Any `@Component` that is marked with `@Profile`
will only be loaded in the profile specified by the latter annotation.

A `SpringApplication` takes this a stage further, in that you can use a
`spring.profiles.active` `Environment` property to specify which profiles are active.
You can specify the property in any of the usual ways, for example you could include
it in your `application.properties`:

```
spring.profiles.active=dev,hsqldb
```

or specify on the command line using the switch `--spring.profiles.active=dev,hsqldb`.

#### Adding active profiles
The `spring.profiles.active` property follows the same ordering rules as other
properties, the highest `PropertySource` will win. This means that you can specify
active profiles in `application.properties` then **replace** them using the command line
switch.

Sometimes it is useful to have profile specific properties that **add** to the active
profiles rather than replace them. The `+` prefix can be used to add active profiles.

For example, when an application with following properties is run using the switch
`--spring.profiles.active=prod` the `proddb` and `prodmq` profiles will also be activated:

```yaml
---
my.property: fromyamlfile
---
spring.profiles: prod
spring.profiles.active: +proddb,+prodmq
```

### Application Context Initializers
Spring provides a convenient `ApplicationContextInitializer` interface that can be used
to customize an `ApplicationContext` before it is used. If you need to use an initializer
with your `SpringApplication` you can use the `addInitializers` method.

You can also specify initializers by setting comma-delimited list of class names to the
`Environment` property `context.initializer.classes` or by using Spring's
`SpringFactoriesLoader` mechanism.


## Embedded Servlet Container Support
Spring Boot introduces a new type of Spring `ApplicationContext` that can be used to
start an embedded servlet container. The `EmbeddedWebApplicationContext`  is a special
type of `WebApplicationContext` that starts the container by searching for a single
`EmbeddedServletContainerFactory` bean contained within itself. We provide
`TomcatEmbeddedServletContainerFactory` and `JettyEmbeddedServletContainerFactory`
 implementations for running embedded Tomcat or Jetty.

One advantage of using a Spring bean to define the embedded container is that you can use
all the standard Spring concepts. For example, it becomes trivial to define a Tomcat
server that sets its port from an injected `@Value`.

```java
@Configuration
public class MyConfiguration {

	@Value("${tomcatport:8080}")
	private int port;

	@Bean
	public EmbeddedServletContainerFactory servletContainer() {
		return new TomcatEmbeddedServletContainerFactory(this.port);
	}

}
```

### Customizing Servlet Containers
Both the Tomcat and Jetty factories extend from the base
`AbstractEmbeddedServletContainerFactory` class. This provides a uniform way
to configure both containers.

Settings that you would traditionally configure in a `web.xml` or via an implementation
specific configuration file can now be performed programmatically.

```java
@Bean
public EmbeddedServletContainerFactory servletContainer() {
	TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
	factory.setPort(9000);
	factory.setSessionTimeout(10, TimeUnit.MINUTES);
	factory.addErrorPages(new ErrorPage(HttpStatus.404, "/notfound.html");
	return factory;
}
```

In addition, you can also add `ServletContextInitializer` implementations which allow
you to customize the `javax.servlet.ServletContext` in the same way as any Servlet 3.0
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


### JSP limitations

When running a Spring Boot application that uses
an embedded servlet container (and is packaged as an executable
archive), there are some limitations in the JSP support.

* With Tomcat it should work if you use WAR packaging, i.e. an
  executable WAR will work, and will also be deployable to a standard
  container (not limited to, but including Tomcat). An executable JAR
  will not work because of a hard coded file pattern in Tomcat.

* Jetty does not currently work as an embedded container with
  JSPs. There should be a way to make it work, so hopefully someone
  can figure it out (pull requests always welcome).

There is a
[JSP sample](https://github.com/spring-projects/spring-boot/tree/master/spring-boot-samples/spring-boot-sample-web-jsp)
so you can see how to set things up.

## Using YAML instead of Properties
[YAML](http://yaml.org) is a superset of JSON, and as such is a very convenient format
for specifying hierarchical configuration data. The `SpringApplication` class will
automatically support YAML as an alternative to properties whenever you have the
[SnakeYAML](http://code.google.com/p/snakeyaml/) library on your classpath.

### Loading YAML
Spring Boot provides two convenient classes that can be used to load YAML documents. The
`YamlPropertiesFactoryBean` will load YAML as `Properties` and the `YamlMapFactoryBean`
will load YAML as a `Map`.

For example, the following YAML document:
```yaml
dev:
	url: http://dev.bar.com
	name: Developer Setup
prod:
	url: http://foo.bar.com
	name: My Cool App
```

Would be transformed into these properties:
```
environments.dev.url=http://dev.bar.com
environments.dev.name=Developer Setup
environments.prod.url=http://foo.bar.com
environments.prod.name=My Cool App
```

 YAML lists are represented as comma-separated values (useful for simple String values)
 and also as property keys with `[index]` dereferencers, for example this YAML:

```yaml
 servers:
 	- dev.bar.com
 	- foo.bar.com
```

Would be transformed into these properties:

```
servers=dev.bar.com,foo.bar.com
servers[0]=dev.bar.com
servers[1]=foo.bar.com
```

### Exposing YAML as properties in the Spring Environment.
The `YamlPropertySourceLoader` class can be used to expose YAML as a `PropertySource`
in the Spring `Environment`. This allows you to the familiar `@Value` with placeholders
syntax to access YAML properties.

You can also specify multiple profile-specific YAML document in a single file by
by using a `spring.profiles` key to indicate when the document applies. For example:

```yaml
server:
	address: 192.168.1.100
---
spring:
	profiles: production
server:
	address: 192.168.1.120
```

### YAML shortcomings
YAML files can't (currently) be loaded via the `@PropertySource` annotation. So in the case
that you need to load values that way, you need to use a properties file.


## Typesafe Configuration Properties
Use the `@Value("${property}")` annotation to inject configuration properties can
sometimes be cumbersome, especially if you are working with multiple properties or
your data is hierarchical in nature. Spring Boot provides an alternative method
of working with properties that allows strongly typed beans to govern and validate
the configuration of your application. For example:

```java
@Component
@ConfigurationProperties(name="connection")
public class ConnectionSettings {

	private String username;

	private InetAddress remoteAddress;

	// ... getters and setters

}
```

When the `@EnableConfigurationProperties` annotation is applied to your `@Configuration`,
any beans annotated with `@ConfigurationProperties` will automatically be configured
from the `Environment` properties. This style of configuration works particularly well
with the `SpringApplication` external YAML configuration:

```yaml
# application.yml

connection:
	username: admin
	remoteAddress: 192.168.1.1

# additional configuration as required
```

To work with `@ConfigurationProperties` beans you can just inject them in the same way
as any other bean.

```java
@Service
public class MyService {

	@Autowired
	private ConnectionSettings connection;

 	//...

	@PostConstruct
	public void openConnection() {
		Server server = new Server();
		this.connection.configure(server);
	}
}
```

It is also possible to shortcut the registration of `@ConfigurationProperties` bean
definitions by simply listing the properties classes directly in the
`@EnableConfigurationProperties` annotation:

```java
@Configuration
@EnableConfigurationProperties(ConnectionSettings.class)
public class MyConfiguration {
}
```

### Relaxed binding
Spring Boot uses some relaxed rules for binding `Environment` properties to
`@ConfigurationProperties` beans, so there doesn't need to be an exact match between
the `Environment` property name and the bean property name.  Common examples where this
is useful include underscore separated (e.g. `context_path` binds to `contextPath`), and
capitalized (e.g. `PORT` binds to `port`) environment properties.

Spring will attempt to coerce the external application properties to the right type when
it binds to the `@ConfigurationProperties` beans. If you need custom type conversion you
can provide a `ConversionService` bean (with bean id `conversionService`) or custom
property editors (via a `CustomEditorConfigurer` bean).

### @ConfigurationProperties Validation
Spring Boot will attempt to validate external configuration, by default using JSR-303
(if it is on the classpath). You can simply add JSR-303 `javax.valididation` constraint
annotations to your `@ConfigurationProperties` class:

```java
@Component
@ConfigurationProperties(name="connection")
public class ConnectionSettings {

	@NotNull
	private InetAddress remoteAddress;

	// ... getters and setters

}
```

You can also add a custom Spring `Validator` by creating a bean definition called
`configurationPropertiesValidator`.

### Using Project Lombok
You can safely use [Project Lombok](http://projectlombok.org) to generate getters and
setters for your `@ConfigurationProperties`. Refer to the documentation on the Lombok
for how to enable it in your compiler or IDE.

### External EmbeddedServletContainerFactory configuration
Spring Boot includes a `@ConfigurationProperties` annotated class called
`ServerProperties` that can be used to configure the `EmbeddedServletContainerFactory`.

When registered as a bean, the `ServerProperties` can be used to specify:

* The port that the application listens on for its endpoints
  (`server.port` defaults to `8080`)
* The address that the application endpoints are available on
  (`server.address` defaults to all local addresses, making it available to connections
  from all clients).
* The context root of the application endpoints (`server.context_path`
  defaults to '/')

If you are using Tomcat as you embedded container then, in addition to the
generic `ServerProperties`, you can also bind `server.tomcat.*` properties
to specify:

* The Tomcat access log pattern (`server.tomcat.accessLogPattern`)
* The remote IP and protocol headers (`server.tomcat.protocolHeader`,
  `server.tomcat.remoteIpHeader`)
* The Tomcat `base directory` (`server.tomcat.basedir`)

## Customizing Logging
Spring Boot uses [Commons Logging](commons.apache.org/logging) for all internal logging,
but leaves the underlying log implementation open. Default configurations are provided for
[Java Util Logging](http://docs.oracle.com/javase/7/docs/api/java/util/logging/package-summary.html),
[Log4J](http://logging.apache.org/log4j/) and [Logback](http://logback.qos.ch/).
In each case there is console output and file output (rotating, 10MB file size).

The various logging systems can be activated by including the appropriate libraries on
the classpath, and further customized by supported by providing a suitable configuration
file in the root of the classpath, or in a location specified by the Spring `Environment`
property `logging.config`.

Depending on your logging system, the following files will be loaded:

|Logging System|Customization                  |
|--------------|-------------------------------|
|Logback       | logback.xml                   |
|Log4j         | log4j.properties or log4j.xml |
|JDK           | logging.properties            |


To help with the customization some other properties are transferred from the Spring
`Environment` to System properties:

|Environment  |System Property |Comments |
|-------------|----------------|---------------------------------------------------------------------------|
|logging.file |LOG_FILE        | Used in default log configuration if defined                              |
|logging.path |LOG_PATH        | Used in default log configuration if defined                              |
|PID          |PID             | The current process ID is discovered if possible and not already provided |

All the logging systems supported can consult System properties when parsing their
configuration files.  See the default configurations in `spring-boot.jar` for examples.

## Cloud Foundry Support
When a `SpringApplication` is deployed to [Cloud Foundry](http://www.cloudfoundry.com/)
appropriate meta-data will be exposed as `Environemnt` properties. All Cloud Foundry
properties are prefixed `vcap.` You can use vcap properties to access application
information (such as the public URL of the application) and service information (such
as database credentials). See `ApplicationContextInitializer` Javdoc for complete details.

## Further Reading
For more information about any of the classes or interfaces discussed in the document
please refer to the extensive project Javadoc. If looking to reduce the amount of
configuration required for your application you should consider
[spring-boot-autoconfigure](../spring-boot-autoconfigure/README.md). For operational
concerns see [spring-boot-actuator](../spring-boot-actuator/README.md). For details on
how to package your application into a single executable JAR file take a look at
[spring-boot-loader](../spring-boot-loader/README.md).

