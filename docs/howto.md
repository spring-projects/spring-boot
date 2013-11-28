# How Do I Do That With Spring Boot?

Here is a starting point for a potentially large collection of micro
HOWTO guides. If you want to add a placeholder for a question without
an answer, put it at the top (at header level 2) and we can fill in
the gaps later.

There is a really useful `AutoConfigurationReport` available in any
Spring Boot `ApplicationContext`. You will see it automatically if a
context fails to start, and also if you enable DEBUG logging for
Spring Boot. If you use the Actuator there is also an endpoint
`/autoconfig` that renders the report in JSON. Use that to debug the
application and see what features have been added (and which not) by
Spring Boot at runtime.

Many more questions can be answered by looking at the source code and
Javadocs. Some rules of thumb:

* Look for classes called `*AutoConfiguration` and read their sources,
  in particular the `@Conditional*` annotations to find out what
  features they enable and when. In those clases...
  
* Look for classes that are `@ConfigurationProperties`
  (e.g. [`ServerProperties`](https://github.com/spring-projects/spring-boot/blob/master/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/web/ServerProperties.java?source=c))
  and read from there the available external configuration
  options. The `@ConfigurationProperties` has a `name` attribute which
  acts as a prefix to external properties, thus `ServerProperties` has
  `name="server"` and its configuration properties are `server.port`,
  `server.address` etc.
  
* Look for use of `RelaxedEnvironment` to pull configuration values
  explicitly out of the `Environment`. It often is used with a prefix.
  
* Look for `@Value` annotations that bind directly to the
  `Environment`. This is less flexible than the `RelaxedEnvironment`
  approach, but does allow some relaxed binding, specifically for OS
  environment variables (so `CAPITALS_AND_UNDERSCORES` are synonyms
  for `period.separated`).
  
* Look for `@ConditionalOnExpression` annotations that switch features
  on and off in response to SpEL expressions, normally evaluated with
  placeholders resolved from the `Environment`.

## Configure Tomcat

## Configure Jetty

<span id="build.hierarchy"/>
## Build an ApplicationContext Hierarchy (Adding a Parent or Root Context)

The
[`SpringApplicationBuilder`](https://github.com/spring-projects/spring-boot/blob/master/spring-boot/src/main/java/org/springframework/boot/builder/SpringApplicationBuilder.java)
has methods specifically designed for the purpose of building a
hierarchy, e.g.

```java
SpringApplicationBuilder application = new SpringApplicationBuilder();
application.sources(Parent.class).child(Application.class).run(args);
```

There are some restrictions, e.g. the parent aplication context is
*not* a `WebApplicationContext`.  Both parent and child are executed
with the same `Environment` constructed in the usual way to include
command line arguments.  Any `ServletContextAware` components all have
to go in the child context, otherwise there is no way for Spring Boot
to create the `ServletContext` in time.

## Convert an Existing Application to Spring Boot

For a non-web application it should be easy (throw away the code that
creates your `ApplicationContext` and replace it with calls to
`SpringApplication` or `SpringApplicationBuilder`). Spring MVC web
applications are generally amenable to first creating a deployable WAR
application, and then migrating it later to an executable WAR and/or
JAR.  Useful reading is in the
[Getting Started Guide on Converting a JAR to a WAR](http://spring.io/guides/gs/convert-jar-to-war/).

Create a deployable WAR by extending `SpringBootServletInitializer`
(e.g. in a class called `Application`), and add the Spring Boot
`@EnableAutoConfiguration` annotation. Example:

```
@Configuration
@EnableAutoConfiguration
@ComponentScan
public class Application extends SpringBootServletInitializer {
	
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(Application.class);
	}

}
```

Remember that whatever you put in the "sources" is just a Spring
`ApplicationContext` and normally anything that already works should
work here. There might be some beans you can remove later and let
Spring Boot provide its own defaults for them, but it should be
possible to get something working first.

Static resources can be moved to `/public` (or `/static` or
`/resources` or `/META-INFO/resources`) in the classpath root. Same
for `messages.properties` (Spring Boot detects this automatically in
the root of the classpath).

Vanilla usage of Spring `DispatcherServlet` and Spring Security should
require no further changes. If you have other features in your
application, using other servlets or filters, for instance then you
may need to add some configuration to your `Application` context,
replacing those elements from the `web.xml` as follows:

* A `@Bean` of type `Servlet` or `ServletRegistrationBean` installs
  that bean in the container as if it was a `<servlet/>` and
  `<servlet-mapping/>` in `web.xml`

* A `@Bean` of type `Filter` or `FilterRegistrationBean` behaves
  similarly (like a `<filter/>` and `<filter-mapping/>`.

* An `ApplicationContext` in an XML file can be added to an `@Import`
  in your `Application`. Or simple cases where annotation
  configuration is heavily used already can be recreated in a few
  lines as `@Bean` definitions.

Once the WAR is working we make it executable by adding a `main`
method to our `Application`, e.g.

```java
public static void main(String[] args) {
	SpringApplication.run(Application.class, args);
}
```

Applications can fall into more than one category:

* Servlet 3.0 applications with no `web.xml`
* Applications with a `web.xml`
* Applications with a context hierarchy and 
* Those without a context hierarchy

All of these should be amenable to translation, but each might require
slightly different tricks.

Servlet 3.0 applications might translate pretty easily if they already
use the Spring Servlet 3.0 initializer support classes. Normally all
the code from an existing `WebApplicationInitializer` can be moved
into a `SpringBootServletInitializer`. If your existing application
has more than one `ApplicationContext` (e.g. if it uses
`AbstractDispatcherServletInitializer`) then you might be able to
squish all your context sources into a single `SpringApplication`. The
main complication you might encounter is if that doesn't work and you
need to maintain the context hierarchy. See the
[entry on building a hierarchy](#build.hierarchy) for examples. An
existing parent context that contains web-specific features will
usually need to be broken up so that all the `ServletContextAware`
components are in the child context.

Applications that are not already Spring applications might be
convertible to a Spring Boot application, and the guidance above might
help, but your mileage may vary.


## Serve Static Content

Spring Boot by default will serve static content from a folder called
`/static` (or `/public` or or `/resources` or `/META-INF/resources`)
in the classpath or from the root of the `ServeltContext`.  It uses
the `ResourceHttpRequestHandler` from Spring MVC so you can modify
that behaviour by adding your own `WebMvcConfigurerAdapter` and
overriding the `addResourceHandlers` method.

By default in a standalone web application the default servlet from
the container is also enabled, and acts as a fallback, serving content
from the root of the `ServletContext` if Spring decides not to handle
it. Most of the time this will not happen unless you modify the
deafult MVC configuration because Spring will always be able to handle
requests through the `DispatcherServlet`.

In addition to the 'standard' static resource locations above, a
special case is made for
[Webjars content](http://www.webjars.org/). Any resources with a path
in `/webjars/**` will be served from jar files if they are packaged in
the Webjars format.

For more detail look at the
[`WebMvcAutoConfiguration`](https://github.com/spring-projects/spring-boot/blob/master/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/web/WebMvcAutoConfiguration.java?source=c)
source code.

## Change the HTTP Port

In a standalone application the main HTTP port defaults to 8080, but
can be set with `server.port` (e.g. in `application.properties` or as
a System property). Thanks to relaxed binding of `Environment` values
you can also use `SERVER_PORT` (e.g. as an OS environment variable).

To scan for a free port (using OS natives to prevent clashes) use
`server.port=0`. To switch off the HTTP endpoints completely, but
still create a `WebApplicationContext`, use `server.port=-1` (this is
sometimes useful for testing).

For more detail look at the
[`ServerProperties`](https://github.com/spring-projects/spring-boot/blob/master/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/web/ServerProperties.java?source=c)
source code.

## Change the HTTP Port or Address of the Actuator Endpoints

In a standalone application the Actuator HTTP port defaults to the
same as the main HTTP port. To make the application listen on a
different port set the external property `management.port`. To listen
on a completely different network address (e.g. if you have an
internal network for management and an external one for user
applications) you can also set `management.address` to a valid IP
address that the server is able to bind to.

For more detail look at the
[`ManagementServerProperties`](https://github.com/spring-projects/spring-boot/blob/master/spring-boot-actuator/src/main/java/org/springframework/boot/actuate/properties/ManagementServerProperties.java?source=c)
source code.

## Use 'Short' Command Line Arguments

Some people like to use (for example) `--port=9000` instead of
`--server.port=9000` to set configuration properties on the command
line. You can easily enable this by using placeholders in
`application.properties`, e.g.

```properties
server.port: ${port:8080}
```

> Note that in this specific case the port binding will work in a PaaS
> environment like Heroku and Cloud Foundry, since in those two
> platforms the `PORT` environment variable is set automatically and
> Spring can bind to capitalized synonyms for `Environment`
> properties.

## Test a Spring Boot Application

A Spring Boot application is just a Spring `ApplicationContext` so
nothing very special has to be done to test it beyond what you would
normally do with a vanilla Spring context. One thing to watch out for
though is that the external properties, logging and other features of
Spring Boot are only installed in the context by default if you use
`SpringApplication` to create it. Spring Boot has a special Spring
`@ContextConfiguration` annotation, so you can use this for example
(from the JPA Sample):

```java
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleDataJpaApplication.class)
public class CityRepositoryIntegrationTests {

	@Autowired
	CityRepository repository;

...
```

To use the `@SpringApplicationConfiguration` you need the test jar on
your classpath (recommended Maven co-ordinates
"org.springframework.boot:spring-boot-starter-test"). The context
loader guesses whether you want to test a web application or not
(e.g. with `MockMVC`) by looking for the `@WebAppConfiguration`
annotation.  (`MockMVC` and `@WebAppConfiguration` are from the Spring
Test support library).

<span id="main.properties"/>
## Externalize the Configuration of SpringApplication

A `SpringApplication` has bean properties (mainly setters) so you can
use its Java API as you create the application to modify its
behaviour. Or you can externalize the configuration using properties
in `spring.main.*`. E.g. in `application.properties` you might have

```properties
spring.main.web_environment: false
spring.main.show_banner: false
```

and then the Spring Boot banner will not be printed on startup, and
the application will not be a web application.

## Create a Non-Web Application

Not all Spring applications have to be web applications (or web
services). If you want to execute some code in a `main` method, but
also bootstrap a Spring application to set up the infrastructure to
use, then it's easy with the `SpringApplication` features of Spring
Boot. A `SpringApplication` changes its `ApplicationContext` class
depending on whether it thinks it needs a web application or not. The
first thing you can do to help it is to just leave the web
depdendencies off the classpath. If you can't do that (e.g. you are
running 2 applications from the same code base) then you can
explicitly call `SpringApplication.setWebEnvironment(false)`, or set
the `applicationContextClass` property (through the Java API or with
[external properties](#main.properties)). Application code that you
want to run as your business logic can be implemented as a
`CommandLineRunner` and dropped into the context as a `@Bean`
definition.

## Create a Deployable WAR File

Use the `SpringBootServletInitializer` base class, which is picked up
by Spring's Servlet 3.0 support on deployment. Add an extension of
that to your project and build a WAR file as normal. For more detail,
see the ["Converting a JAR Project to a WAR" guide][gs-war] on the
spring.io website.

The WAR file can also be executable if you use the Spring Boot build
tools. In that case the embedded container classes (to launch Tomcat
for instance) have to be added to the WAR in a `lib-provided`
directory. The tools will take care of that as long as the
dependencies are marked as "provided" in Maven or Gradle. Here's a
Maven example
[in the Boot Samples](https://github.com/spring-projects/spring-boot/blob/master/spring-boot-samples/spring-boot-sample-traditional/pom.xml).

[gs-war]: http://spring.io/guides/gs/convert-jar-to-war

## Create a Deployable WAR File for older Servlet Containers

Older Servlet containers don't have support for the
`ServletContextInitializer` bootstrap process used in Servlet 3.0. You
can still use Spring and Spring Boot in these containers but you are
going to need to add a `web.xml` to your application and configure it
to load an `ApplicationContext` via a `DispatcherServlet`.

TODO: add some detail.

## Discover Built-in Options for External Properties

Spring Boot binds external properties from `application.properties`
(or `.yml`) (and other places) into an application at runtime.  There
is not (and technically cannot be) an exhaustive list of all supported
properties in a single location because contributions can come from
additional JAR files on your classpath.  There is a sample
[`application.yml`](https://github.com/spring-projects/spring-boot/blob/master/docs/application.yml)
with a non-exhaustive and possibly inaccurate list of properties
supported by Spring Boot vanilla with autoconfiguration. The
definitive list comes from searching the source code for
`@ConfigurationProperties` and `@Value` annotations, as well as the
occasional use of `RelaxedEnvironment`
(c.f. [here](https://github.com/spring-projects/spring-boot/blob/master/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/orm/jpa/HibernateJpaAutoConfiguration.java?source=c#L65)).

## Set the Active Spring Profiles

The Spring `Environment` has an API for this, but normally you would
set a System profile (`spring.profiles.active`) or an OS environment
variable (`SPRING_PROFILES_ACTIVE`). E.g. launch your application with
a `-D...` argument (remember to put it before the main class or jar
archive):

```
java -jar -Dspring.profiles.active=production demo-0.0.1-SNAPSHOT.jar
```

In Spring Boot you can also set the active profile in
`application.properties`, e.g.

```properties
spring.profiles.active: production
```

A value set this is replaced by the System property or environment
variable setting, but not by the `SpringApplicationBuilder.profiles()`
method. Thus the latter Java API can be used to augment the profiles
without changing the defaults.

## Change the Location of External Properties of an Application

Properties from different sources are added to the Spring
`Environment` in a defined order, and the precedence for resolution is
1) commandline, 2) filesystem (current working directory)
`application.properties`, 3) classpath `application.properties`. To
modify this you can provide System properties (or environment variables) 

* `config.name` (`CONFIG_NAME`), defaults to `application` as the root
  of the file name
* `config.location` (`CONFIG_LOCATION`) is a comma-separated list of
  files to load. A separate `Environment` property source is set up
  for each document found, so the priority order is most significant
  first. Defaults to
  `file:./application.properties,classpath:application.properties`. If
  YAML is used then those files are also added to the list by default.

See `ConfigFileApplicationContextInitializer` for more detail.

## Use YAML for External Properties

YAML is a superset of JSON and as such is a very convenient syntax for
storing external properties in a hierarchical format. E.g.

```yaml
spring:
  application:
    name: cruncher
  datasource:
    driverClassName: com.mysql.jdbc.Driver
    url: jdbc:mysql://localhost/test
server:
  port: 9000
```

Create a file called `application.yml` and stick it in the root of
your classpath, and also add `snake-yaml` to your classpath (Maven
co-ordinates `org.yaml:snake-yaml`). A YAML file is parsed to a Java
`Map<String,Object>` (like a JSON object), and Spring Boot flattens
the maps so that it is 1-level deep and has period-separated keys, a
lot like people are used to with `Properties` files in Java.

The example YAML above corresponds to an `application.properties` file

```properties
spring.application.name: cruncher
spring.datasource.driverClassName: com.mysql.jdbc.Driver
spring.datasource.url: jdbc:mysql://localhost/test
server.port: 9000
```

## Change Configuration Depending on the Environment

A YAML file is actually a sequence of documents separated by `---`
lines, and each document is parsed separately to a flattened map.

If a YAML document contains a `spring.profiles` key, then the
profiles value (comma-separated list of profiles) is fed into the
Spring `Environment.acceptsProfiles()` and if any of those profiles is
active that document is included in the final merge (otherwise not).

Example:

```yaml
server:
  port: 9000

---

spring:
  profiles: development
server:
  port: 9001

---

spring:
  profiles: production
server:
  port: 0
```

In this example the default port is 9000, but if the Spring profile
"development" is active then the port is 9001, and if "production" is
active then it is 0.

The YAML documents are merged in the order they are encountered (so
later values override earlier ones).

To do the same thing with properties files you can use
`application-${profile}.properties` to specify profile-specific
values.

