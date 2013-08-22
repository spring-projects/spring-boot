# Spring Actuator Feature Guide

Here are some (most, hopefully all) the features of Spring Actuator 
with some commentary to help you start using them.  We
recommend you first build a project with the Actuator (e.g. the
getting started project from the main README), and then try each
feature in turn there.

Many useful features of
[Spring Boot](../../spring-boot/README.md) are all available
in an Actuator application.

TODO: group things together and break them out into separate files.

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

    $ curl user:password@localhost:8080/metrics
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
