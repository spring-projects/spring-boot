# Spring Zero Actuator Feature Guide

Here are some (most, hopefully all) the features of Spring Zero
Actuator with some commentary to help you start using them.  We
recommend you first build a project with the Actuator (e.g. the
getting started project from the main README), and then try each
feature in turn there.

TODO: some of these are features of Spring Zero (or
`SpringApplication`) not the Actuator.

TODO: group things together and break them out into separate files.

## Commandline Arguments

Commandline arguments are passed on to any `CommandLineRunner` beans
found in the application.  Option arguments (starting with `--`,
e.g. `--server.port=9000`) are converted to a `PropertySource` and
added to the Spring `Environment` with first priority (they always
take precedence and override values from other sources).  Properties
in the `Environment` (including System properties and OS environment
variables) can always be injected into Spring components using
`@Value` with placeholders, e.g.

    @Component
    public class MyService {
        @Value("${app.message:Hello World}")
        private String message;
        ...
    }

The default value comes after the first colon (":").

## Externalized Configuration

In addition to command line option arguments, Spring Zero will
pick up a file called `application.properties` in the root of your
classpath (if there is one) and add those properties to the Spring
`Environment`.  The search path for `application.properties` is
actually, 1) root or classpath, 2) current directory, 3) `/config`
package in classpath, 4) `/config` subdir of current directory.  The
list is ordered by decreasing precedence (so properties can be
overridden by others with the same name defined in later locations).

The values in `application.properties` are filtered through the
existing `Environment` when they are used so you can refer back to
previously defined values (e.g. from System properties), e.g.

    app.name: MyApp
    app.description: ${app.name} is a Cool New App

Spring Zero also binds the properties to any bean in your
application context whose type is `@ConfigurationProperties`.  The
Actuator provides some of those beans out of the box, so you can
easily customize server and management properties (ports etc.),
endpoint locations and logging.  See below for more detail, or inspect
the `*Properties` types in the Actuator jar.

## Setting the Default Spring Profile

Spring Profiles are a way to segregate parts of the application
configuration and make it only available in certain environments.  Any
`@Component` that is marked with `@Profile` will only be loaded in the
profile specified by the latter annotation.

Spring Zero takes it a stage further.  If you include in your
`application.properties` a value for a property named
`spring.active.profiles` then those profiles will be active by
default.  E.g.

    spring.active.profiles: dev,hsqldb

## Profile-dependent configuration

Spring Zero loads additional properties files if there are active
profiles using a naming convention `application-{profile}.properties`.
Property values from those files override trhe default ones.

## Custom Typesafe Externalized Configuration

If you want a strongly typed bean (or beans) to govern and validate
the configuration of your application beyond the built in properties,
all you need to do is create a `@ConfigurationProperties` class, e.g.

    @ConfigurationProperties(name="my")
    public class MyProperties {
    }

and declare one either explicitly (with `@Bean`) or implicitly by
adding

    @EnableConfigurationProperties(MyProperties.class)

to one of your `@Configuration` (or `@Component`) classes.  Then you can

    @Autowired
    private MyProperties configuration = new MyProperties();

in any of your component classes to grab that configuration and use it.

Spring Zero uses some relaxed rules for binding `Environment`
properties to `@ConfigurationProperties` beans, so there doesn't need
to be an exact match between the `Environment` property name and the
bean property name.  Common examples where this is useful include
underscore separated (e.g. `context_path` binds to `contextPath`), and
capitalized (e.g. `PORT` binds to `port`) environment properties.

Spring will attempt to coerce the external application properties to
the right type when it binds to the `@ConfigurationProperties` beans.
If you need custom type conversion you can provide a
`ConversionService` bean (with bean id `conversionService`) or custom
property editors (via a `CustomEditorConfigurer` bean).

Spring will also validate the external configuration, by default using
JSR-303 if it is on the classpath.  So you can add annotations from
that specification (or its implementations) to your custom properties,
e.g.

    @ConfigurationProperties(name="my")
    public class MyProperties {
        @NotNull
        private String name;
        // .. getters and setters
    }

You can also add a custom Spring `Validator` by creating a bean
definition called `configurationPropertiesValidator`.

## Using Project Lombok

You can safely use [Project Lombok](http://projectlombok.org) to
generate getters and setters for your `@ConfigurationProperties`.
Refer to the documentation on the Lombok for how to enable it in your
compiler or IDE.

## Using YAML instead of Properties

YAML is a superset of JSON, and as such is a very convenient format
for specifying hierarchical configuration data, such as that supported
by Spring Zero Actuator.  If you prefer to use
[YAML](http://yaml.org) instead of Properties files you just need to
include a file called `application.yml` in the root of your classpath

You can if you like add profile specific YAML files
(`application-${profile}.yml`), but a nicer alternative is to use YAML
documents inside `application.yml`, with profile-specific documents
containing a `spring.profiles` key.  For example

    server:
      port: 8080
    management:
      port: 8080
      address: 0.0.0.0
    ---
    spring:
      profiles: prod
    management:
      port: 8081
      address: 10.2.68.12

## Customizing the location of the External Configuration

If you don't like `application.properties` or `application.yml` as the
configuration file location you can switch to another location by
specifying the `spring.config.name` (default `application`) or the
`spring.config.location` as environment properties, e.g. if launching
a jar which wraps `SpringApplication`:

    $ java -jar myproject.jar --spring.config.name=myproject

## Providing Defaults for Externalized Configuration

For `@ConfigurationProperties` beans that are provided by the
framework itself you can always change the values that are bound to it
by changing `application.properties`.  But it is sometimes also useful
to change the default values imperatively in Java, so get more control
over the process.  You can do this by declaring a bean of the same
type in your application context, e.g. for the server properties:

    @AssertMissingBean(ServerProperties.class)
    @Bean
    public ServerProperties serverProperties() {
        ServerProperties server = new ServerProperties();
        server.setPort(8888);
        return server;
    }

Note the use of `@AssertMissingBean` to guard against any mistakes
where the bean is already defined (and therefore might already have
been bound).

## Server Configuration

The `ServerProperties` are bound to application properties, and
can be used to specify

* The port that the application listens on for the its endpoints
  (`server.port` defaults to 8080)

* The address that the application endpoints are available on
  (`server.address` defaults to all local addresses, making it available to connections
  from all clients).

* The context root of the application endpoints (`server.context_path`
  defaults to "/")

## Tomcat Container Configuration

If you want to use Tomcat as an embedded container include at least
`org.apache.tomcat.embed:tomcat-embed-core` and one of the
`org.apache.tomcat.embed:tomcat-embed-logging-*` libraries (depending
on the logging system you are using).  Then, in addition to the
generic `ServerProperties`, you can also bind `server.tomcat.*`
properties in the application properties (see
`ServerProperties.Tomcat`).

* To enable the Tomcat access log valve (very common in production environments)

More fine-grained control of the Tomcat container is available if you
need it.  Instead of letting Spring Zero create the container for
you, just create a bean of type
`TomcatEmbeddedServletContainerFactory` and override one of its
methods, or inject some customizations, e.g.

    @Configuration
    public class MyContainerConfiguration {
        @Bean
        public TomcatEmbeddedServletContainerFactory tomcatEmbeddedContainerFactory() {
            TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
            factory.setConnector(new Connector("AJP/1.3"));
        }
    }

(the default connector uses the `Http11NioProtocol` so the example if
overriding that behaviour).

## Customizing Management Endpoints

The `ManagementProperties` are bound to application properties, and
can be used to specify

* The port that the application listens on for the management
  endpoints (defaults to 8080)

* The address that the management endpoints are available on (if the
  port is different to the main server port).  Use this to listen only
  on an internal or ops-facing network, for instance, or to only
  listen for connections from localhost (by specifying "127.0.0.1")

* The context root of the management endpoints

## Error Handling

The Actuator provides an `/error` mapping by default that handles all
errors in a sensible way.  If you want more specific error pages for
some conditions, the embedded servlet containers support a uniform
Java DSL for customizing the error handling.  To do this you have to
have picked a container implementation (by including either Tomcat or
Jetty on the classpath), but then the API is the same.  TODO: finish
this.

## Customizing Logging

Spring Zero uses SLF4J for logging, but leaves the implementation
open.  The Starter projects and the Actuator use JDK native logging by
default, purely because it is always available.  A default
configuration file is provided for JDK logging, and also for log4j and
logback.  In each case there is console output and file output
(rotating, 10MB file size).

The various logging systems can be activated by including the right
libraries on the classpath, and further customized by providing a
native configuration file in the root of the classpath, or in a
location specified by the Spring `Environment` property
`logging.config`.

|Logger|Activation |Customization |
|---|---|---|
|JDK   |slf4j-jdk14  | logging.properties |
|Logback |logback  | logback.xml |
|Log4j   |slfj4-log4j12, log4j  | log4j.properties or log4j.xml |

To help with the customization some other properties are transferred
from the Spring `Environment` to System properties:

|Environment|System Property |Comments |
|---|---|---|
|logging.file   |LOG_FILE  | Used in default log configuration if defined |
|logging.path   |LOG_PATH  | Used in default log configuration if defined |
|PID            |PID       | The current process ID is discovered if possible and not already provided |

All the logging systems supported can consult System properties when
parsing their configuration files.  See the default configurations in
`spring-zero-core.jar` for examples.

## Application Context Initializers

To add additional application context initializers to the Zero
startup process, add a comma-delimited list of class names to the
`Environment` property `context.initializer.classes` (can be specified
via `application.properties`).

## Info Endpoint

By default the Actuator adds an `/info` endpoint to the main server.
It contains the commit and timestamp information from `git.properties`
(if that file exists) and also any properties it finds in the
environment with prefix "info".

To populate `git.properties` in a
Maven build you can use the excellent
[git-commit-id-plugin](https://github.com/ktoso/maven-git-commit-id-plugin).

To populate the "info" map all you need to do is add some stuff to
`application.properties`, e.g.

    info.app.name: MyService
    info.app.description: My awesome service
    info.app.version: 1.0.0

If you are using Maven you can automcatically populate info properties
from the project using resource filtering.  In your `pom.xml` you
have (inside the `<build/>` element):

        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
            </resource>
        </resources>

and then in the `application.properties` you can refer to project
properties via placeholders, e.g.

    project.artifactId: myproject
    project.name: Demo
    project.version: X.X.X.X
    project.description: Demo project for info endpoint
    info.build.artifact: ${project.artifactId}
    info.build.name: ${project.name}
    info.build.description: ${project.description}
    info.build.version: ${project.version}

(notice that in the example we used `project.*` to set some values to
be used as fallbacks if the Maven resource filtering has for some
reason not been switched on).

## Security - Basic Authentication

To secure your endpoints just add Spring Security Javaconfig to the
classpath.  By default HTTP Basic authentication will be applied to
every request in the main server (and the management server if it is
running on the same port).  There is a single account by default, and
you can test it like this:

    $ mvn user:password@localhost:8080/metrics
    ... stuff comes out

If the management server is running on a different port it is
unsecured by default.  If you want to secure it you can add a security
auto configuration explicitly

## Security - HTTPS

Ensuring that all your main endpoints are only available over HTTPS is
an important chore for any application.  If you are using Tomcat as a
servlet container, then the Actuator will add Tomcat's own
`RemoteIpValve` automatically if it detects some environment settings,
and you should be able to rely on the `HttpServletRequest` to report
whether or not it is secure (even downstream of the real SSL
termination endpoint).  The standard behaviour is determined by the
presence or absence of certain request headers ("x-forwarded-for" and
"x-forwarded-proto"), whose names are conventional, so it should work
with most front end proxies.  You switch on the valve by adding some
entries to `application.properties`, e.g.

    server.tomcat.remote_ip_header: x-forwarded-for
    server.tomcat.protocol_header: x-forwarded-proto

(The presence of either of those properties will switch on the
valve. Or you can add the `RemoteIpValve` yourself by adding a
`TomcatEmbeddedServletContainerFactory` bean.)

Spring Security can also be configured to require a secure channel for
all (or some requests). To switch that on in an Actuator application
you just need to set `security.require_https: true` in
`application.properties`.

## Audit Events

The Actuator has a flexible audit framework that will publish events
once Spring Security is in play (authentication success and failure
and access denied exceptions by default).  This can be very useful for
reporting, and also to implement a lock-out policy based on
authentication failures.

You can also choose to use the audit services for your own business
events.  To do that you can either inject the existing
`AuditEventRepository` into your own components and use that directly,
or you can simply publish `AuditApplicationEvent` via the Spring
`ApplicationContext` (using `ApplicationEventPublisherAware`).

## Metrics Customization

Metrics come out on the `/metrics` endpoint.  You can add additional
metrics by injecting a `MetricsRepository` into your application
components and adding metrics whenever you need to.  To customize the
`MetricsRepository` itself, just add a bean definition of that type to
the application context (only in memory is supported out of the box
for now).

## Customizing the Health Indicator

The application always tells you if it's healthy via the `/health`
endpoint.  By default it just responds to a GET witha 200 status and a
plain text body containing "ok".  If you want to add more detailed
information (e.g. a description of the current state of the
application), just add a bean of type `HealthIndicator` to your
application context, and it will take the place of the default one.
