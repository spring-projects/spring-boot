# How Do I Do That With Spring Boot?

Here is a starting point for a potentially large collection of micro
HOWTO guides. If you want to add a placeholder for a question without
an answer, put it at the top (at header level 2) and we can fill in
the gaps later.

## Configure Tomcat

## Create a Non-Web Application

## Create a Deployable WAR File?

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

## Create a Deployable WAR File for older Servlet Containers?

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

