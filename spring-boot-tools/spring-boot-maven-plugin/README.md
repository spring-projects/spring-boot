# Spring Boot - Maven Plugin

> **Note:** We are currently still working on documentation for Spring Boot. This 
> README is not yet complete, please check back in the future.

A maven plugin for building executable JAR and WAR files. To use it,
configure your project to build a JAR or WAR (as appropriate) in the
normal way, and then add the Spring plugin to your `<build><plugins>`
section

`pom.xml`

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-package-maven-plugin</artifactId>
    <version>{{project.version}}</version>
    <executions>
	    <execution>
            <goals>
		        <goal>package</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

The net effect of that is to enhance your existing archive with the
Spring Launcher during the Maven `package` phase. The main class will
be selected from the existing `MANIFEST.MF` if there is one, or else
the plugin will attempt to guess based on the contents of the local
`src/main/java` source tree.

So to build and run a project artifact you do something like this:

```
$ mvn package
$ java -jar target/*.jar
...
<application runs>
```
