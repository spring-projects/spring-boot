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
attempt to rewrite archives to make them executable using the `bootRepackage` task. You
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

### Running a Project in Place

To run a project in place without building a jar first you can use the "bootRun" task:

```
$ gradle bootRun
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

### Repackage with Custom Gradle Configuration
Sometimes it may be more appropriate to not package default dependencies resolved from
`compile`, `runtime` and `provided` scopes. If created executable jar file
is intended to be run as it is you need to have all dependencies in it, however
if a plan is to explode a jar file and run main class manually you may already
have some of the libraries available via `CLASSPATH`. This is a situation where
you can repackage boot jar with a different set of dependencies. Using a custom
configuration will automatically disable dependency resolving from 
`compile`, `runtime` and `provided` scopes. Custom configuration can be either
defined globally inside `springBoot` or per task.

```groovy
task clientJar(type: Jar) {
	appendix = 'client'
	from sourceSets.main.output
	exclude('**/*Something*')
}

task clientBoot(type: BootRepackage, dependsOn: clientJar) {
	withJarTask = clientJar
	customConfiguration = "mycustomconfiguration"
}
```
In above example we created a new `clientJar` Jar task to package a customized
file set from your compiled sources. Then we created a new `clientBoot`
BootRepackage task and instructed it to work with only `clientJar` task and
`mycustomconfiguration`.

```groovy
configurations {
	mycustomconfiguration.exclude group: 'log4j'
}

dependencies {
	mycustomconfiguration configurations.runtime
}
```
Configuration we are referring to in `BootRepackage` is a normal
Gradle configuration. In above example we created a new configuration
named `mycustomconfiguration` instructing it to derive from a `runtime`
and exclude `log4j` group. If `clientBoot` task is executed, repackaged
boot jar will have all dependencies from a runtime but no
log4j jars.

The following configuration options are available:


| Name                  | Type    | Description                                                                                                                                                                | Default Value   |
|-----------------------|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------|
| mainClass             | String  | The main class that should be run. If not specified the value from the manifest will be used, or if no manifest entry is the archive will be searched for a suitable class |                 |
| providedConfiguration | String  | The name of the provided configuration                                                                                                                                     | providedRuntime |
| backupSource          | boolean | If the original source archive should be backed-up before being repackaged                                                                                                 | true            |
| customConfiguration | String  | The name of the custom configuration                                                                                                                                         | none            |

## Further Reading
For more information on how Spring Boot Loader archives work, take a look at the
[spring-boot-loader](../spring-boot-loader) module. If you prefer using Maven to
build your projects we have a [spring-boot-maven-plugin](../spring-boot-maven-plugin).

### Understanding how Boot Gradle Plugin Works
When `spring-boot` is applied to your Gradle project a default task
named `bootRepackage` is created automatically. Boot repackage task
depends on Gradle `assemble` task and when executed, it tries to find
all jar artifacts whose qualifier is empty(meaning i.e. tests and
sources jars are automatically skipped).

Because on default every repackage task execution will find all
created jar artifacts, the order of Gradle task execution is
important. This is not going to be an issue if you have a normal 
project setup where only one jar file is created. However if you are
planning to create more complex project setup with custom Jar and
BootRepackage tasks, there are few tweaks to consider.

```groovy
jar.enabled = false
bootRepackage.enabled = false
```
Above example simply disables default `jar` and `bootRepackage` tasks.
This would be all right if you are just creating custom jar files
out from your project. You could also just disable default
`bootRepackage` task.

```groovy
bootRepackage.withJarTask = jar
```
Above example simply instructs default `bootRepackage` task to only
work with a default `jar` task. 


```groovy
task bootJars
bootJars.dependsOn = [clientBoot1,clientBoot2,clientBoot3]
build.dependsOn(bootJars)
```
If you still have a default project setup where main jar file is
created and repackaged to be used with boot and you still want to
create additional custom jar files out from your project, you
could simple combine you custom repackage tasks together and
create dependency to your build so that `bootJars` task would
be run after the default `bootRepackage` task is executed.

All the above tweaks are usually used to avoid situation where
already created boot jar is repackaged again. Repackaging 
an existing boot jar will not break anything but you may
get unnecessary dependencies in it.
