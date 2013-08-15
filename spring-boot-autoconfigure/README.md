# Spring Boot - AutoConfigure
Spring Boot AutoConfiguration attempts to automatically configure your Spring application
based on the dependencies that it declares.  For example, If `HSQLDB` is on your
classpath, and you have not manually configured any database connection beans, then we
will auto-configure an in-memory database.

##Enabling auto-configuration
Add an `@EnableAutoConfiguration` annotation to your primary `@Configration` class to
enable auto-configuration:

```java
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;

@Configuration
@EnableAutoConfiguration
public class MyConfiguration {
}
```

Currently auto-configuration is provided for the following types of application:

* Web (Tomcat or Jetty, Spring MVC)
* JDBC (Commons DBCP, embedded databases, jdbcTemplate)
* JPA with Hibernate
* Spring Data JPA (automatically detecting `Repository` classes)
* Spring Batch (including `JobLauncherCommandLineRunner`s and database initialization)
* Thymeleaf templating
* Reactor asynchronous JVM programming

###Understanding auto-configured beans
Under the hood, auto-configuration is implemented with standard `@Configuration` classes.
Additional `@Conditional` annotations are used to constrain when the auto-configuration
should apply. Usually auto-configuration classes use `@ConditionalOnClass` and
`@ConditionalOnMissingBean` annotations. This ensures that auto-configuration only
applies when relevant classes are found and when you have not declared your own
`@Configuration`.

You can browse the source code of `spring-boot-autoconfigure` to see the `@Configuration`
classes that we provide (see the `META-INF/spring.factories` file).

> **Note:** If you are using `org.springframework.boot.SpringApplication`, you can see
> which `@Conditions` were not applied by starting your application with the `--debug`
> option.

###Disabling specific auto-configuration
All auto-configuration that we provide attempts to back away as you start to define your
own beans. If, however, you find that specific auto-configure classes are being applied
that you don't want you can use the `exclude` attribute of `@EnableAutoConfiguration`
to disable them.

```java
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.jdbc.*;
import org.springframework.context.annotation.*;

@Configuration
@EnableAutoConfiguration(exclude={EmbeddedDatabaseConfiguration.class})
public class MyConfiguration {
}
```
##Condition annotations
Spring Boot Auto-Configure includes a number of `@Conditional` annotations that are used
limit when auto-configure is applied. You can reuse these in your own code by annotating
`@Configuration` classes or individual `@Bean` methods.

###Class conditions
The `ConditionalOnClass` and `ConditionalOnMissingClass` annotations allow configuration
to be skipped based on the presence or absence of specific classes. Due to the fact that
annotation meta-data is parsed using ASM you can actually use the `value` attribute to
refer to the real class, even though that class might not actually appear in the running
application classpath. You can also use the `name` attribute if you prefer to specify
the class name using a `String` value.

###Bean conditions
The `@ConditionalOnBean` and `@ConditionalOnMissingBean` annotations allow configuration
to be skipped based on the presence or absence of specific beans. You can use the `value`
attribute to specify beans by type, or `name` to specify beans by name. The `search`
attribute allows you to limit the `ApplicationContext` hierarchy that should be considered
when searching for beans.

> **Note:** `@Conditional` annotations are processed when `@Configuration` classes are
parsed. Auto-configure `@Configuration` is always parsed last (after any user defined
beans), however, if you are using these annotations on regular `@Configuration` classes,
care must be take not to refer to bean definitions that have not yet been created.

###Resource conditions
The `@ConditionalOnResource` annotation allows configuration to be skipped when a specific
resource is not present. Resources can be specified using the usual Spring conventions,
for example, `file:C:/test.dat`.

###Web Application Conditions
The `@ConditionalOnWebApplication` and `@ConditionalOnNotWebApplication` annotations
allow configuration to be skipped depending on whether the application is a
'web application'. A web application is any application that is using a Spring
`WebApplicationContext`, defines a `session` scope or has a `StandardServletEnvironment`.

###SpEL expression conditions
The `@ConditionalOnExpression` annotation allows configuration to be skipped based on the
result of a SpEL expression.

##Writing additional auto-configuration
You can easily add auto-configuration to your own libraries, simply create a regular
`@Configuration` class and annotate it with appropriate `@Conditional` restrictions.

Spring Boot checks for presence of a `META-INF/spring.factories` file within your
published jar. The file should list your configuration classes under the
`org.springframework.boot.autoconfigure.EnableAutoConfiguration` key.

```
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.mycorp.libx.autoconfigure.LibXAutoConfiguration,\
com.mycorp.libx.autoconfigure.LibXWebAutoConfiguration
```

You can use the `@AutoConfigureAfter` or `@AutoConfigureBefore` annotations if your
configuration needs to be applied in a specific order. For example, if you provide
web specific configuration you may need to be applied after `WebMvcAutoConfiguration`.

#Further reading
For more information about any of the classes or interfaces discussed in the document
please refer to the extensive project Javadoc. If you are just starting out with Spring
Boot take a look at the [getting started](../README.md) guide.
