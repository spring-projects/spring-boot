# Spring Boot - Maven Plugin 

The Spring Boot Maven Plugin provides Spring Boot support in Maven,
allowing you to package executable jar or war archives and run an
application in-place. To use it you must be using Maven 3 (or better).

## Including the plugin
To use the Spring Boot Maven Plugin simply include the appropriate XML in the `plugins`
section of your `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<!-- ... -->
	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<version>{{project.version}}</version>
				<executions>
					<execution>
						<goals>
							<goal>repackage</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
</project>
```

This configuration will repackage a JAR or WAR that is built in the
"package" phase of the Maven lifecycle, so

```
$ mvn package
$ ls target/*.jar
target/myproject-1.0.0.jar target/myproject-1.0.0.jar.original
```

will reveal the result. If you don't include the `<execution/>`
configuration as above you can run the plugin on its own, but only if
the package goal is used as well, e.g.

```
$ mvn package spring-boot:repackage
```

will have the same effect as above.

If you are using a milestone or snapshot release you will also need to add appropriate
`pluginRepository` elements:

```xml
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

## Packaging executable jar and war files
Once `spring-boot-maven-plugin` has been included in your `pom.xml` it will
automatically attempt to rewrite archives to make them executable using the
`spring-boot:repackage` goal. You should configure your project to build a jar or war
(as appropriate) using the usual `packaging` element:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<!-- ... -->
	<packaging>jar</packaging>
	<!-- ... -->
</project>
```

Your existing archive will be enhanced by Spring Boot during the `package`
phase. The main class that you want to launch can either be specified using a
configuration option, or by adding a `Main-Class` attribute to the manifest in the usual
way. If you don't specify a main class the plugin will search for a class with a
`public static void main(String[] args)` method.

To build and run a project artifact, you do something like this:

```
$ mvn package
$ java -jar target/mymodule-0.0.1-SNAPSHOT.jar
```


### Repackage configuration
The following configuration options are available for the `spring-boot:repackage` goal:

**Required Parameters**

| Name            | Type   | Description                                | Default Value              |
|-----------------|--------|--------------------------------------------|----------------------------|
| outputDirectory | File   | Directory containing the generated archive | ${project.build.directory} |
| finalName       | String | Name of the generated archive              | ${project.build.finalName} |


**Optional Parameters**

| Name            | Type   | Description                                                                                                                                                                              |
|-----------------|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| classifier      | String | Classifier to add to the artifact generated. If given, the artifact will be attached. If this is not given, it will merely be written to the output directory according to the finalName |
| mainClass       | String | The name of the main class. If not specified the first compiled class found that contains a 'main' method will be used                                                                   |


## Running applications
The Spring Boot Maven Plugin includes a `run` goal which can be used to launch your
application from the command line. Type the following from the root of your maven
project:

```
$ mvn spring-boot:run
```

By default, any `src/main/resources` folder will be added to the application classpath
when you run via the maven plugin. This allows hot refreshing of resources which can be
very useful when web applications. For example, you can work on HTML, CSS or JavaScipt
files and see your changes immediately without recompiling your application. It is also
a helpful way of allowing your front end developers to work without needing to download
and install a Java IDE.

### Run configuration
The following configuration options are available for the `spring-boot:run` goal:

**Required Parameters**

| Name                                 | Type    | Description                                                                                  | Default Value                    |
|--------------------------------------|---------|----------------------------------------------------------------------------------------------|----------------------------------|
| classesDirectrory                    | File    | Directory containing the classes and resource files that should be packaged into the archive | ${project.build.outputDirectory} |


**Optional Parameters**

| Name                                 | Type     | Description                                                                                                                                                                                                                                                                                                                                               | Default Value                    |
|--------------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------|
| arguments (or -Drun.arguments)       | String[] | Arguments that should be passed to the application                                                                                                                                                                                                                                                                                                        |                                  |
| addResources (or -Drun.addResources) | boolean  | Add maven resources to the classpath directly, this allows live in-place editing or resources. Since resources will be added directly, and via the target/classes folder they will appear twice if ClassLoader.getResources() is called. In practice however most applications call ClassLoader.getResource() which will always return the first resource | true                             |
| mainClass                            | String   | The name of the main class. If not specified the first compiled class found that contains a 'main' method will be used                                                                                                                                                                                                                                    |                                  |
| folders                              | String[] | Folders that should be added to the classpath                                                                                                                                                                                                                                                                                                             | ${project.build.outputDirectory} |


## Further Reading
For more information on how Spring Boot Loader archives work, take a look at the
[spring-boot-loader](../spring-boot-loader) module. If you prefer using Gradle to
build your projects we have a [spring-boot-gradle-plugin](../spring-boot-gradle-plugin).
