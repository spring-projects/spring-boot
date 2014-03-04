# Spring Boot - Starters

Starters are a set of convenient dependency descriptors that you can
include in your application. You get a one-stop-shop for all the
Spring and related technology that you need without having to hunt
through sample code and copy paste loads of dependency
descriptors. For example, if you want to get started using Spring and
JPA for database access just include the
`spring-boot-starter-data-jpa` dependency in your project, and you are
good to go.  The starters contain a lot of the dependencies that you
need to get a project up and running quickly and with a consistent,
supported set of managed transitive dependencies.

## Building a Spring Boot Project with Maven

The quickest way to get started with a new project is to use the
spring-boot-starter-parent, e.g.

```xml
	<!-- Inherit defaults from Spring Boot -->
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>1.0.0.RC4</version>
	</parent>
```

The parent pom adds two main fetaures to your project:

* dependency management configuration, so you don't have to specify
  versions or excludes with your own dependencies, as long as they are
  part of the Spring Boot stack

* plugin configuration, so you don't have to configure some common
  settings in the main Maven plugins used to get a project off the
  ground (e.g. the
  [Spring Boot Maven plugin](../spring-boot-tools/spring-boot-maven-plugin/README.md))

As an example, if you want to build a simple RESTful web service with
embedded Tomcat, you only need one dependency:

```xml
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
	</dependencies>
```

And if you want to use the Spring Boot plugin to package the project
as an executable JAR (or run it from source code), you only need to
add the plugin (not configure it, unless you want to change the
settings in the parent):

```xml
	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
    </build>
```

If you need to access milestone or snapshot builds from Spring
projects, you can optionallyt add repositories, as follows:

```xml
	<repositories>
		<repository>
			<id>spring-snapshots</id>
			<url>http://repo.spring.io/snapshot</url>
			<snapshots><enabled>true</enabled></snapshots>
		</repository>
		<repository>
			<id>spring-milestones</id>
			<url>http://repo.spring.io/milestone</url>
			<snapshots><enabled>true</enabled></snapshots>
		</repository>
	</repositories>
	<pluginRepositories>
		<pluginRepository>
			<id>spring-snapshots</id>
			<url>http://repo.spring.io/snapshot</url>
		</pluginRepository>
		<pluginRepository>
			<id>spring-milestones</id>
			<url>http://repo.spring.io/milestone</url>
		</pluginRepository>
	</pluginRepositories>
```

The repositories cannot all be defined in the parent (at least one
will be required to resolve the parent itself).

### Using Your Own Parent POM

If you don't want to use the Spring Boot starter parent, you can use
your own and still keep the benefit of the dependency management (but
not the plugin management) using
["scope=import"](http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html). Example:

```xml
<dependencyManagement>
  <dependencies>
	<dependency>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>1.0.0.RC4</version>
        <scope>import</scope>
	</dependency>
  </dependencies>
</dependencyManagement>
```

(It actually doesn't matter if you use "spring-boot-starter-parent" or
"spring-boot-dependencies".)

### Samples

All the
[Spring Boot Samples](../spring-boot-samples)
come with a `pom.xml`, but they use a different parent to
spring-boot-starter-parent (although that pom is one of their
ancestors). Other useful places to get clean projects with a
`pom.xml` that works as quickly as possible:

* The
[Getting Started Guides on spring.io](http://spring.io/guides/gs) are
all available in [github](https://github.com/spring-guides), or you
can copy paste from the guides themselves (all the code is on the web
page).

* The same guides are available as working projects in
[Spring Tool Suite](http://spring.io/tools/sts)(STS) (an Eclipse
plugin feature set), and so is a starter project template generator
(via `File->New->Spring Starter Project`).

* The Spring Starter Project feature in STS is backed by a web
application at [start.spring.io](http://start.spring.io), which you
can also use yourself to download ZIP files of the starter projects.

## Building With Gradle

If you prefer to build your project with Gradle then similar features
are available to you as to Maven users. The starter projects work in
exactly the same way, for instance, so a typical project might have a
`build.gradle` like this:

```groovy
buildscript {

    ext {
		springBootVersion = '1.0.0.RC4'
	}

    repositories {
	    mavenCentral()
		maven { url "http://repo.spring.io/libs-milestone" }
	}
	dependencies {
		classpath("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")
	}
}


apply plugin: 'java'
apply plugin: 'spring-boot'

jar {
	baseName = 'spring-boot-sample-simple'
	version =  '0.5.0'
}

repositories {
	mavenCentral()
	maven { url "http://repo.spring.io/libs-milestone" }
}

dependencies {
	compile("org.springframework.boot:spring-boot-starter-web")
	testCompile("org.springframework.boot:spring-boot-starter-test")
}

task wrapper(type: Wrapper) { gradleVersion = '1.6' }
```

This build is for a simple RESTful web service (with embedded
Tomcat). Notice that the dependencies do not have explicit version
numbers. This works for Spring Boot dependencies, but also anything
else that is explicitly managed by the Spring Boot Starter parent.

Versionless dependency resolution is a feature of the
[Spring Boot Gradle Plugin](../spring-boot-tools/spring-boot-gradle-plugin/README.md),
which also gives you a "run from source" feature ("gradle bootRun")
and an enhanced JAR/WAR packaging feature for executable archives
("gradle build").

### Samples

Most of the samples mentioned above for Maven users are also buildable
using Gradle. Only some of the
[Spring Boot Samples](../spring-boot-samples)
come with a `build.gradle`. The Spring Starter Project feature in STS
produces a Maven project. But web application at
[start.spring.io](http://start.spring.io) can generate Gradle build
files for the same projects (just download it with a browser or
command line).

