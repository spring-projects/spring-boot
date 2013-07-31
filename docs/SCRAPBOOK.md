# Docs without a home as yet :(


|Feature |Implementation |Notes |
|---|---|---|
|Launch Spring from Java main |SpringApplication | Plenty of convenience methods and customization opportunities |
|Server   |Tomcat or Jetty  | |
|Logging  |Logback, Log4j or JDK | Sensible defaults configurations. |
|Externalized configuration | Properties or YAML | Support for Spring profiles. Bind automatically to @Bean. |

For a quick introduction and to get started quickly with a new
project, carry on reading.  For more in depth coverage of the features
of Spring Boot.Strap, go to the [Feature Guide](docs/Features.md).

# Getting Started

You will need Java (6 at least) and a build tool (Maven is what we use
below, but you are more than welcome to use gradle).  These can be
downloaded or installed easily in most operating systems.  For Ubuntu:

    $ sudo apt-get install openjdk-6-jdk maven

<!--FIXME: short instructions for Mac.-->

## A basic project

If you are using Maven create a really simple `pom.xml` with 2 dependencies:

`pom.xml`

```
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.mycompany</groupId>
  <artifactId>myproject</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>jar</packaging>
  <properties>
      <start-class>com.mycompany.Application</start-class>
  </properties>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-starter-parent</artifactId>
    <version>{{project.version}}</version>
  </parent>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-starter</artifactId>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-package-maven-plugin</artifactId>
     </plugin>
    </plugins>
  </build>
</project>
```

If you like Gradle, that's fine, and you will know what to do with
those dependencies.  The one dependency adds Spring Boot.Config auto
configuration and the Tomcat container to your application.  If you
prefer Jetty you can just add the embedded Jetty jars to your
classpath instead of Tomcat.

Now write a simple main class

`Application.java`
```
package com.mycompany;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
```

You should be able to run it already:

    $ mvn package
    $ java -jar target/myproject-1.0.0-SNAPSHOT.jar

      .   ____          _            __ _ _
     /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
    ( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
     \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
      '  |____| .__|_| |_|_| |_\__, | / / / /
     =========|_|==============|___/=/_/_/_/
     Spring Bootstrap

    2013-07-19 17:13:51.673  INFO 18937 --- [           main] com.mycompany.Application ...
    ... <logs showing application starting up>

It doesn't do anything yet, but that's because all we did is start a
Spring `ApplicationContext` and let it close when the JVM stopped.

To make it do something a little bit more interesting you could bind
some command line arguments to the application:

`Application.java`
```
@Configuration
@ConfigurationProperties
@EnableConfigurationProperties
public class Application {

    private String message;

    @Override
    public void run(String... args) throws Exception {
    	System.err.println(message);
    }

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

 	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
```

The `@ConfigurationProperties` annotation binds the Spring
`Environment` (including command line arguments) to the `Application`
instance, and `CommandLineRunner` is a marker interface for anything
you want to be executed after the content is started. So run it
again and you will see the message:

```
    $ mvn package
    $ java -jar target/myproject-1.0.0-SNAPSHOT.jar --message="Hello World"
    ...
    Hello World
```

To add more features, add some `@Bean` definitions to your
`Application` class, and read more in the
[Feature Guide](docs/Features.md).