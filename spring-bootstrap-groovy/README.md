# Spring Bootstrap Groovy

Spring Bootstrap Groovy gives you the quickest possible getting
started experience with Spring apps, whether you are writing a web
app, a batch job or a standalone java app.

## Building and Testing

To avoid problems with classpaths and existing JVM-based build tools,
Spring Bootstrap Groovy uses an exec plugin call to launch `groovyc`.
You need to have a `sh` on your path along with `groovyc` (2.1.x),
`find` and `xargs`.  These tools are standard on a Mac or Linux
distribution, and available using Cygwin on Windows.  Once it is
built, the zip file is portable.

Here are the steps to build and test:

    $ mvn install

The `spring` executable is then available at
`spring-bootstrap-groovy/target/spring-<VERSION>`. There is also a jar
file with the Groovy Bootstrap components.  The `spring` executable
includes jars from `SPRING_HOME` in the classpath so you can run it
while you are developing like this

    $ export SPRING_HOME=<spring-bootstrap-groovy>/target
    $ <spring-bootstrap-groovy>/src/main/scripts/spring App.groovy
