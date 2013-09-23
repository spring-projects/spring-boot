# Spring Boot - Gradle Plugin
The Spring Boot Gradle Plugin provides Spring Boot support in Gradle, allowing you to
package executable jar or war archives.

## Including the plugin
To use the Spring Boot Gradle Plugin simply include a `buildscript` dependency and apply
the `spring-boot` plugin:

```groovy
buildscript {
	dependencies {
		classpath("org.springframework.boot:spring-boot-gradle-plugin:{{project.version}}")
	}
}
apply plugin: 'spring-boot'
```
If you are using a milestone or snapshot release you will also need to add appropriate
`repositories` reference:

```groovy
buildscript {
	repositories {
		maven.url "http://repo.spring.io/snapshot"
		maven.url "http://repo.spring.io/milestone"
	}
	// ...
}
```

## Packaging executable jar and war files
Once the `spring-boot` plugin has been applied to your project it will automatically
attempt to rewrite archives to make them executable using the `repackage` task. You
should configure your project to build a jar or war (as appropriate) in the usual way.

The main class that you want to launch can either be specified using a configuration
option, or by adding a `Main-Class` attribute to the manifest. If you don't specify a
main class the plugin will search for a class with a
`public static void main(String[] args)` method.

To build and run a project artifact, you do something like this:

```
$ gradle build
$ java -jar build/libs/mymodule-0.0.1-SNAPSHOT.jar
```


### Repackage configuration
The gradle plugin automatically extends your build script DSL with a `springBoot` element
for configuration. Simply set the appropriate properties as you would any other gradle
extension:

```groovy
springBoot {
	backupSource = false
}
```

The following configuration options are available:


| Name                  | Type    | Description                                                                                                                                                                | Default Value   |
|-----------------------|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------|
| mainClass             | String  | The main class that should be run. If not specified the value from the manifest will be used, or if no manifest entry is the archive will be searched for a suitable class |                 |
| providedConfiguration | String  | The name of the provided configuration                                                                                                                                     | providedRuntime |
| backupSource          | boolean | If the original source archive should be backed-up before being repackaged                                                                                                 | true            |

## Further Reading
For more information on how Spring Boot Loader archives work, take a look at the
[spring-boot-loader](../spring-boot-loader) module. If you prefer using Maven to
build your projects we have a [spring-boot-maven-plugin](../spring-boot-maven-plugin).
