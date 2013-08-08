# Contributing to Spring Boot
Spring Boot is released under the non-restrictive Apache 2.0 license. If you would like
to contribute something, or simply want to hack on the code this document should help
you get started.

Before we accept a non-trivial patch or pull request we will need you to sign the
[contributor's agreement](https://support.springsource.com/spring_committer_signup).
Signing the contributor's agreement does not grant anyone commit rights to the main repository, but it does mean that we
can accept your contributions, and you will get an author credit if we do.  Active contributors might be asked to join
the core team, and given the ability to merge pull requests.

## Code Conventions and Housekeeping

None of these is essential for a pull request, but they will all help.  They can also be added after the original pull
request but before a merge.

* Use the Spring Framework code format conventions. Import `eclipse-code-formatter.xml` from the root of the project
  if you are using Eclipse. If using IntelliJ, copy `spring-intellij-code-style.xml` to `~/.IntelliJIdea*/config/codestyles`
  and select spring-intellij-code-style from Settings -> Code Styles.
* Make sure all new .java files to have a simple Javadoc class comment with at least an @author tag identifying you, and
  preferably at least a paragraph on what the class is for.
* Add the ASF license header comment to all new .java files (copy from existing files in the project)
* Add yourself as an @author to the .java files that you modify substantially (moew than cosmetic changes).
* Add some Javadocs and, if you change the namespace, some XSD doc elements.
* A few unit tests would help a lot as well - someone has to do it.
* If no-one else is using your branch, please rebase it against the current master (or other target branch in the main project).

## Working with the code
If you don't have an IDE preference we would recommend that you use
[Spring Tools Suite](http://www.springsource.com/developer/sts) or
[Eclipse](http://eclipse.org) when working with the code. We use the
[m2eclipe](http://eclipse.org/m2e/) eclipse plugin for maven support. Other IDEs
and tools should also work without issue.

### Building from source
To build the source you will need to install
[Apache Maven](http://maven.apache.org/run-maven/index.html) v3.0 or above. The project
can be build using the standard maven command:

	$ mvn clean install

If you are rebuilding often, you might also want to skip the tests until you are ready
to submit a pull request:

	$ mvn clean install -DskipTests

### Importing into eclipse with m2eclipse
We recommend the [m2eclipe](http://eclipse.org/m2e/) eclipse plugin when working with
eclipse. If you don't already have m2eclipse installed it is available from the "eclipse
marketplace".

Spring Boot includes project specific source formatting settings, in order to have these
work with m2eclipse, we provide an additional eclipse plugin that you can install:

* Select `Install new software` from the `help` menu
* Click `Add...` to add a new repository
* Click the `Archive...` button
* Select `org.eclipse.m2e.maveneclipse.site-0.0.1-SNAPSHOT-site.zip`
  from the `eclipse` folder in this checkout
* Install "Maven Integration for the maven-eclipse-plugin"

_NOTE: This plugin is optional. Projects can be imported without the plugin, your code
changes just won't be automatically formatted._

With the requisite eclipse plugins installed you can select
`import existing maven projects` from the `file` menu to import the code. You will
need to import the root `spring-boot` pom and the `spring-boot-samples` pom separately.

### Importing into eclipse without m2eclipse
If you prefer not to use m2eclipse you can generate eclipse project meta-data using the
following command:

	$ mvn eclipse:eclipse

The generated eclipse projects can be imported by selecting `import existing projects`
from the `file` menu.

### Importing into other IDEs
Maven is well supported by most Java IDEs. Refer to you vendor documentation.

### Integration tests
The sample application are used as integration tests during the build (when you 
`mvn install`). Due to the fact that they make use of the `spring-boot-maven-plugin` 
they cannot be called directly, and so instead are launched via the 
`maven-invoker-plugin`. If you encounter build failures running the integration tests, 
check the `build.log` file in the appropriate sample directory.

