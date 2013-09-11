# Spring Boot - Actuator

> **Note:** Some of this documentation covers concepts from other modules, it will be
> cleaned up before the final release.

The aim of this project is minimum fuss for getting applications up
and running in production, and in other environments.  There is a
strong emphasis on implementing RESTful web services but many features
are more generic than that.

|Feature |Implementation |Notes |
|---|---|---|
|Server   |Tomcat or Jetty  | Whatever is on the classpath |
|REST     |Spring MVC       | |
|Security |Spring Security  | If on the classpath |
|Logging  |Logback, Log4j or JDK | Whatever is on the classpath. Sensible defaults. |
|Database |HSQLDB or H2     | Per classpath, or define a DataSource to override |
|Externalized configuration | Properties or YAML | Support for Spring profiles. Bind automatically to @Bean. |
|Audit                      | Spring Security and Spring ApplicationEvent |Flexible abstraction with sensible defaults for security events |
|Validation                 | JSR-303    |If on the classpath |
|Management endpoints       | Spring MVC | Health, basic metrics, request tracing, shutdown, thread dumps |
|Error pages                | Spring MVC | Sensible defaults based on exception and status code |
|JSON                       |Jackson 2 | |
|ORM                        |Spring Data JPA | If on the classpath |
|Batch                      |Spring Batch | If enabled and on the classpath |
|Integration Patterns       |Spring Integration | If on the classpath |

For a quick introduction and to get started quickly with a new
project, carry on reading.  For more in depth coverage of the features
of Spring Boot Actuator, go to the
[Feature Guide](docs/Features.md).

# Getting Started

You will need Java (6 at least) and a build tool (Maven is what we use
below, but you are more than welcome to use gradle).  These can be
downloaded or installed easily in most operating systems.  For Ubuntu:

    $ sudo apt-get install openjdk-6-jdk maven

<!--FIXME: short instructions for Mac.-->

## A basic project

If you are using Maven create a really simple `pom.xml` with 2 dependencies:

    <project>
      <modelVersion>4.0.0</modelVersion>
      <groupId>com.mycompany</groupId>
      <artifactId>myproject</artifactId>
      <version>1.0.0-SNAPSHOT</version>
      <packaging>jar</packaging>
      <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>{{project.version}}</version>
      </parent>
      <dependencies>
        <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
      </dependencies>
      <build>
        <plugins>
          <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
         </plugin>
        </plugins>
      </build>
    </project>

If you like Gradle, that's fine, and you will know what to do with
those dependencies.  The first dependency adds Spring Boot auto
configuration and the Tomcat container to your application, and the
second one adds some more opinionated stuff like the default
management endpoints.  If you prefer Jetty you can just add the
embedded Jetty jars to your classpath instead of Tomcat (once you
exclude the `spring-starter-tomcat` dependency).

## Adding a business endpoint

To do something useful to your business you need to add at least one
endpoint.  An endpoint can be implemented as a Spring MVC
`@Controller`, e.g.

    @Controller
    @EnableAutoConfiguration
    public class SampleController {

      @RequestMapping("/")
      @ResponseBody
      public Map<String, String> helloWorld() {
        return Collections.singletonMap("message", "Hello World");
      }

      public static void main(String[] args) throws Exception {
        SpringApplication.run(SampleController.class, args);
      }

    }

You can use the main method to launch it from your project jar.  You
can also launch that straight using the Spring Boot CLI (without
the `@EnableAutoConfiguration` and even without the import statements
that your IDE will add if you are using one), if you just add

```
@Grab("org.springframework.boot:spring-boot-starter-actuator:{{project.version}}")
```

and package and run:

    $ mvn package
    $ java -jar target/myproject-1.0.0-SNAPSHOT.jar
    $ curl localhost:8080/
    {"message": "Hello World"}

There are also some endpoins that you didn't implement by came free
with the Actuator:

    $ curl localhost:8080/health
    ok
    $ curl localhost:8080/metrics
    {"counter.status.200.health":1.0,"gauge.response.health":10.0,"mem":120768.0,"mem.free":105012.0,"processors":4.0}

`/health` is the default location for the health endpoint - it tells
you if the application is running and healthy. `/metrics` is the default
location for the metrics endpoint - it gives you basic counts and
response timing data by default but there are plenty of ways to
customize it.  You can also try `/trace` and `/dump` to get some
interesting information about how and what your app is doing.

## Running the application

You can package the app and run it as a jar (as above) and that's very
convenient for production usage.  Or there are other options, many of
which are more convenient at development time.  Here are a few:

1. Use the Maven exec plugin, e.g.

        $ mvn exec:java

2. Run directly in your IDE, e.g. Eclipse or IDEA let you right click
on a class and run it.

3. Use a different Maven plugin.

4. Find feature in Gradle that does the same thing.

5. Use the Spring executable.  <!--FIXME: document this maybe.-->

## Externalizing configuration

Spring Boot likes you to externalize your configuration so you
can work with the same application code in different environments.  To
get started with this you create a file in the root of your classpath
(`src/main/resources` if using Maven) - if you like YAML you can call
it `application.yml`, e.g.:

    server:
      port: 9000
    management:
      port: 9001
    logging:
      file: target/log.out

or if you like Java `Properties` files, you can call it
`application.properties`, e.g.:

    server.port: 9000
    management.port: 9001
    logging.file: target/log.out

Those examples are properties that Spring Boot itself binds to
out of the box, so if you make that change and run the app again, you
will find the home page on port 9000 instead of 8080:

    $ curl localhost:9000/
    {"message": "Hello World"}

and the management endpoints on port 9001 instead of 8080:

    $ curl localhost:9001/health
    ok

To externalize business configuration you can simply add a default
value to your configuration file, e.g.

    server:
      port: 9000
    management:
      port: 9001
    logging:
      file: target/log.out
    service:
      message: Awesome Message

and then bind to it in the application code.  The simplest way to do
that is to simply refer to it in an `@Value` annotation, e.g.

    @Controller
    @EnableAutoConfiguration
    public class SampleController {

      @Value("${service.message:Hello World}")
      private String value = "Goodbye Everypone"

      @RequestMapping("/")
      @ResponseBody
      public Map<String, String> helloWorld() {
        return Collections.singletonMap("message", message);
      }

      ...
    }

That's a little bit confusing because we have provided a message value
in three different places - in the external configuration ("Awesome
Message"), in the `@Value` annotation after the colon ("Hello World"),
and in the filed initializer ("Goodbye Everyone").  That was only to
show you how and you only need it once, so it's your choice (it's
useful for unit testing to have the Java initializer as well as the
external value).  Note that the YAML object is flattened using period
separators.

For simple Strings where you have sensible defaults `@Value` is
perfect, but if you want more and you like everything strongly typed
then you can have Spring bind the properties and validate them
automatically in a separate value object.  For instance:

    // ServiceProperties.java
    @ConfigurationProperties(name="service")
    public class ServiceProperties {
        private String message;
        private int value = 0;
        ... getters and setters
    }

    // SampleController.java
    @Controller
    @EnableAutoConfiguration
    @EnableConfigurationProperties(ServiceProperties.class)
    public class SampleController {

      @Autowired
      private ServiceProperties properties;

      @RequestMapping("/")
      @ResponseBody
      public Map<String, String> helloWorld() {
        return Collections.singletonMap("message", properties.getMessage());
      }

      ...
    }

When you ask to
`@EnableConfigurationProperties(ServiceProperties.class)` you are
saying you want a bean of type `ServiceProperties` and that you want
to bind it to the Spring Environment.  The Spring Environment is a
collection of name-value pairs taken from (in order of decreasing
precedence) 1) the command line, 2) the external configuration file,
3) System properties, 4) the OS environment.  Validation is done based
on JSR-303 annotations by default provided that library (and an
implementation) is on the classpath.

## Adding security

If you add Spring Security java config to your runtime classpath you
will enable HTTP basic authentication by default on all the endpoints.
In the `pom.xml` it would look like this:

        <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

Try it out:

    $ curl localhost:8080/
    {"status": 403, "error": "Forbidden", "message": "Access Denied"}
    $ curl user:password@localhost:8080/
    {"message": "Hello World"}

The default auto configuration has an in-memory user database with one
entry.  If you want to extend or expand that, or point to a database
or directory server, you only need to provide a `@Bean` definition for
an `AuthenticationManager`, e.g. in your `SampleController`:



    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return new AuthenticationManagerBuilder(
                ObjectPostProcessor.QUIESCENT_POSTPROCESSOR).inMemoryAuthentication().withUser("user")
                .password("password").roles("USER").and().and().build();
    }

Try it out:

    $ curl user:password@localhost:8080/
    {"status": 403, "error": "Forbidden", "message": "Access Denied"}
    $ curl client:secret@localhost:8080/
    {"message": "Hello World"}

## Adding a database

Just add `spring-jdbc` and an embedded database to your dependencies:

        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.hsqldb</groupId>
            <artifactId>hsqldb</artifactId>
        </dependency>

Then you will be able to inject a `DataSource` into your controller:

    @Controller
    @EnableAutoConfiguration
    @EnableConfigurationProperties(ServiceProperties.class)
    public class SampleController {

      private JdbcTemplate jdbcTemplate;

      @Autowired
      public SampleController(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
      }

      @RequestMapping("/")
      @ResponseBody
      public Map<String, String> helloWorld() {
        return jdbcTemplate.queryForMap("SELECT * FROM MESSAGES WHERE ID=?", 0);
      }

      ...
    }

 The app will run (going back to the default security configuration):

           $ curl user:password@localhost:8080/
           {"error":"Internal Server Error", "status":500, "exception":...}

 but there's no data in the database yet and the `MESSAGES` table
 doesn't even exist, so there's an error.  One easy way to fix it is
 to provide a `schema.sql` script in the root of the classpath, e.g.

    create table MESSAGES (
      ID BIGINT NOT NULL PRIMARY KEY,
      MESSAGE VARCHAR(255)
    );
    INSERT INTO MESSAGES (ID, MESSAGE) VALUES (0, 'Hello Phil');

Now when you run the app you get a sensible response:

       $ curl user:password@localhost:8080/
       {"ID":0, "MESSAGE":"Hello Phil"}

Obviously, this is only the start, but hopefully you have a good grasp
of the basics and are ready to try it out yourself.
