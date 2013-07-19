# Spring Package Maven Plugin

A maven plugin for building executable JAR and WAR files. To use it
configure your project to build a JAR or WAR (as appropriate) in the
normal way, using the `maven-jar-plugin` or `maven-war-plugin`, and
then add the Spring plugin to your `<build><plugins>` section

`pom.xml`
```xml
<plugin>
    <groupId>org.springframework.zero</groupId>
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
