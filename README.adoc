= Spring Boot image:https://github.com/spring-projects/spring-boot/actions/workflows/build-and-deploy-snapshot.yml/badge.svg?branch=main["Build Status", link="https://github.com/spring-projects/spring-boot/actions/workflows/build-and-deploy-snapshot.yml?query=branch%3Amain"] image:https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A["Revved up by Develocity", link="https://ge.spring.io/scans?&search.rootProjectNames=Spring%20Boot%20Build&search.rootProjectNames=spring-boot-build"]

:docs: https://docs.spring.io/spring-boot
:github: https://github.com/spring-projects/spring-boot

Spring Boot helps you to create Spring-powered, production-grade applications and services with absolute minimum fuss.
It takes an opinionated view of the Spring platform so that new and existing users can quickly get to the bits they need.

You can use Spring Boot to create stand-alone Java applications that can be started using `java -jar` or more traditional WAR deployments.
We also provide a command-line tool that runs Spring scripts.

Our primary goals are:

* Provide a radically faster and widely accessible getting started experience for all Spring development.
* Be opinionated, but get out of the way quickly as requirements start to diverge from the defaults.
* Provide a range of non-functional features common to large classes of projects (for example, embedded servers, security, metrics, health checks, externalized configuration).
* Absolutely no code generation and no requirement for XML configuration.



== Installation and Getting Started

The {docs}[reference documentation] includes detailed {docs}/installing.html[installation instructions] as well as a comprehensive {docs}/tutorial/first-application/index.html[``getting started``] guide.

Here is a quick teaser of a complete Spring Boot application in Java:

[source,java]
----
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.web.bind.annotation.*;

@RestController
@SpringBootApplication
public class Example {

	@RequestMapping("/")
	String home() {
		return "Hello World!";
	}

	public static void main(String[] args) {
		SpringApplication.run(Example.class, args);
	}

}
----



== Getting Help

Are you having trouble with Spring Boot? We want to help!

* Check the {docs}/[reference documentation], especially the {docs}/how-to/index.html[How-to's] -- they provide solutions to the most common questions.
* Learn the Spring basics -- Spring Boot builds on many other Spring projects; check the https://spring.io[spring.io] website for a wealth of reference documentation.
  If you are new to Spring, try one of the https://spring.io/guides[guides].
* If you are upgrading, read the {github}/wiki[release notes] for upgrade instructions and "new and noteworthy" features.
* Ask a question -- we monitor https://stackoverflow.com[stackoverflow.com] for questions tagged with https://stackoverflow.com/tags/spring-boot[`spring-boot`].
* Report bugs with Spring Boot at {github}/issues[github.com/spring-projects/spring-boot/issues].



== Contributing

We welcome contributions of all kinds!
Please read our link:CONTRIBUTING.adoc[contribution guidelines] before submitting a pull request.



== Reporting Issues

Spring Boot uses GitHub's integrated issue tracking system to record bugs and feature requests.
If you want to raise an issue, please follow the recommendations below:

* Before you log a bug, please search the {github}/issues[issue tracker] to see if someone has already reported the problem.
* If the issue doesn't already exist, {github}/issues/new[create a new issue].
* Please provide as much information as possible with the issue report.
We like to know the Spring Boot version, operating system, and JVM version you're using.
* If you need to paste code or include a stack trace, use Markdown.
+++```+++ escapes before and after your text.
* If possible, try to create a test case or project that replicates the problem and attach it to the issue.



== Building from Source

You don't need to build from source to use Spring Boot.
If you want to try out the latest and greatest, Spring Boot can be built and published to your local Maven cache using the https://docs.gradle.org/current/userguide/gradle_wrapper.html[Gradle wrapper].
You also need JDK 25.

[source,shell]
----
$ ./gradlew publishToMavenLocal
----

This command builds all modules and publishes them to your local Maven cache.
It won't run any of the tests.
If you want to build everything, use the `build` task:

[source,shell]
----
$ ./gradlew build
----



== Guides

The https://spring.io/[spring.io] site contains several guides that show how to use Spring Boot step-by-step:

* https://spring.io/guides/gs/spring-boot/[Building an Application with Spring Boot] is an introductory guide that shows you how to create an application, run it, and add some management services.
* https://spring.io/guides/gs/actuator-service/[Building a RESTful Web Service with Spring Boot Actuator] is a guide to creating a REST web service and also shows how the server can be configured.



== License

Spring Boot is Open Source software released under the https://www.apache.org/licenses/LICENSE-2.0.html[Apache 2.0 license].
