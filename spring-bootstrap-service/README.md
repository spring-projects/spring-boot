<style>
table
{
border-collapse:collapse;
}
table,th, td
{
border: 1px solid black;
}
</style>

# Spring Bootstrap Services

Minimum fuss for getting RESTful services up and running in
production, and in other environments.

|Feature |Implementation |Notes |
|---|---|---|
|Server   |Tomcat or Jetty  | Whatever is on the classpath |
|REST     |Spring MVC       | |
|Security |Spring Security  | If on the classpath |
|Logging  |Logback, Log4j or JDK | Whatever is on the classpath. Sensible defaults. |
|Database |HSQLDB or H2     | Per classpath, or define a DataSource to override |
|Externalized configuration | Properties or YAML | Support for Spring profiles. Bind automatically to @Bean. |
|Audit                      | Spring Security and Spring ApplicationEvent |Flexible abstraction with sensible defaults for security events |
|Validation                 | JSR-303    | |
|Management endpoints       | Spring MVC | Health, basic metrics, request tracing, shutdown, thread dumps |
|Error pages                | Spring MVC | Sensible defaults based on exception and status code |
|JSON                       |Jackson 2 | |
|ORM                        |Spring Data JPA | If on the classpath |
|Batch                      |Spring Batch | If enabled and on the classpath |
|Integration Patterns       |Spring Integration | If on the classpath |

# Getting Started

You will need Java (6 at least) and a build tool (Maven is what we use
below, but you are more than welcome to use gradle).  These can be
downloaded or installed easily in most operating systems.  FIXME:
short instructions for Mac and Linux.

## A basic project

If you are using Maven create a really simple `pom.xml` with 2 dependencies:

    <project>
      <modelVersion>4.0.0</modelVersion>
      <groupId>com.mycompany</groupId>
      <artifactId>myproject</artifactId>
      <version>1.0.0-SNAPSHOT</version>
      <packaging>jar</packaging>
      <parent>
        <groupId>org.springframework.bootstrap</groupId>
        <artifactId>spring-bootstrap-applications</artifactId>
        <version>0.0.1-SNAPSHOT</version>
      </parent>
      <properties>
         <spring.bootstrap.version>0.0.1-SNAPSHOT</spring.bootstrap.version>
         <start-class>org.springframework.bootstrap.SpringApplication</start-class>
      </properties>
      <dependencies>
        <dependency>
          <groupId>org.springframework.bootstrap</groupId>
          <artifactId>spring-bootstrap-web-application</artifactId>
          <version>${spring.bootstrap.version}</version>
        </dependency>
        <dependency>
          <groupId>org.springframework.bootstrap</groupId>
          <artifactId>spring-bootstrap-service</artifactId>
          <version>${spring.bootstrap.version}</version>
        </dependency>
      </dependencies>
      <build>
        <plugins>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
         </plugin>
        </plugins>
      </build>
    </project>

If you like Gradle, that's fine, and you will know what to do with
those dependencies.  The first dependency adds Spring Bootstrap auto
configuration and the Jetty container to your application, and the
second one adds some more opinionated stuff like the default
management endpoints.  If you prefer Tomcat FIXME: use a different
dependency.

You should be able to run it already:

    $ mvn package
    $ java -jar target/myproject-1.0.0-SNAPSHOT.jar

Then in another terminal

    $ curl localhost:8080/healthz
    ok
    $ curl localhost:8080/varz
    {"counter.status.200.healthz":1.0,"gauge.response.healthz":10.0,"mem":120768.0,"mem.free":105012.0,"processors":4.0}
    
`/healthz` is the default location for the health endpoint - it tells
you if the application is running and healthy. `/varz` is the default
location for the metrics endpoint - it gives you basic counts and
response timing data by default but there are plenty of ways to
customize it.  You can also try `/trace` and `/dump` to get some
interesting information about how and what your app is doing.

What about the home page?

    $ curl localhost:8080/
    {"status": 404, "error": "Not Found", "message": "Not Found"}

That's OK, we haven't added any business content yet.  But it shows
that there are sensible defaults built in for rendering HTTP and
server-side errors.

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

You can launch that straight away using the Spring Bootstrap CLI
(without the `@EnableAutoConfiguration` and even without the import
statements that your IDE will add if you are using one), or you can
use the main method to launch it from your project jar.  Just change
the `start-class` in the `pom` above to the fully qualified name of
your `SampleController`, e.g.

    <start-class>com.mycompany.sample.SampleController</start-class>

and re-package:

    $ mvn package
    $ java -jar target/myproject-1.0.0-SNAPSHOT.jar
    $ curl localhost:8080/
    {"message": "Hello World"}

# Adding security

If you add Spring Security java config to your runtime classpath you
will enable HTTP basic authentication by default on all the endpoints.
In the `pom.xml` it would look like this:

        <dependency>
          <groupId>org.springframework.security</groupId>
          <artifactId>spring-security-javaconfig</artifactId>
          <version>1.0.0.BUILD-SNAPSHOT</version>
        </dependency>

(Spring Security java config is still work in progress so we have used
a snapshot.  Beware of sudden changes.  FIXME: update to full
release.)

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
      return new AuthenticationBuilder().inMemoryAuthentication().withUser("client")
          .password("secret").roles("USER").and().and().build();
    }

Try it out:

    $ curl user:password@localhost:8080/
    {"status": 403, "error": "Forbidden", "message": "Access Denied"}
    $ curl client:secret@localhost:8080/
    {"message": "Hello World"}

# Adding a database

Just add `spring-jdbc` and an embedded database to your dependencies:

FIXME: TBD
