# Spring Boot - CLI

## Installing the CLI

You need [Java SDK v1.6](http://www.java.com) or higher to run the command line tool
(there are even some issues with the `1.7.0_25` build of openjdk, so stick to earlier
builds or use `1.6` for preference). You should check your current Java installation
before you begin:

	$ java -version

### Manual installation
You can download the Spring CLI distribution from the Spring software repository:

* [spring-boot-cli-0.5.0.M5-bin.zip](http://repo.spring.io/milestone/org/springframework/boot/spring-boot-cli/0.5.0.M5/spring-boot-cli-0.5.0.M5-bin.zip)
* [spring-boot-cli-0.5.0.M5-bin.tar.gz](http://repo.spring.io/milestone/org/springframework/boot/spring-boot-cli/0.5.0.M5/spring-boot-cli-0.5.0.M5-bin.tar.gz)

Cutting edge [snapshot distributions](http://repo.spring.io/snapshot/org/springframework/boot/spring-boot-cli/)
are also available.

Once downloaded, follow the
[INSTALL](spring-boot-cli/src/main/content/INSTALL.txt) instructions
from the unpacked archive. In summary: there is a `spring` script
(`spring.bat` for Windows) in a `bin/` directory in the `.zip` file,
or alternatively you can use `java -jar` with the `.jar` file (the
script helps you to be sure that the classpath is set correctly).

### Installation with GVM

GVM (the Groovy Environment Manager) can be used for managing multiple
versions of verious Groovy and Java binary packages, including Groovy
itself and the Spring Boot CLI. Get `gvm` from
[the gvm home page](http://gvmtool.net) and install Spring Boot with

    $ gvm install springboot
    $ spring --version
    Spring Boot v0.5.0.M5

> **Note:** If you are developing features for the CLI and want easy access to the version you just built, follow these extra instructions.

    $ gvm install springboot dev /path/to/spring-boot/spring-boot-cli/target/spring-boot-cli-0.5.0.BUILD-SNAPSHOT-bin/spring-0.5.0.BUILD-SNAPSHOT/
   	$ gvm use springboot dev
   	$ spring --version
    Spring CLI v0.5.0.BUILD-SNAPSHOT

This will install a local instance of `spring` called the `dev` instance inside your gvm repository. It points at your target build location, so every time you rebuild Spring Boot, `spring` will be up-to-date.

You can see it by doing this:

    $ gvm ls springboot

```
================================================================================
Available Springboot Versions
================================================================================
 > + dev
   * 0.5.0.M5

================================================================================
+ - local version
* - installed
> - currently in use
================================================================================
```

### OSX Homebrew installation
If you are on a Mac and using [homebrew](http://brew.sh/), all you need to do to install
the Spring Boot CLI is:

```
$ brew install http://repo.spring.io/install/spring-boot-cli.rb
```

Homebrew will install `spring` to `/usr/local/bin`. Now you can jump right to a
[quick start example](#quick-start-script-example).

> **Note:** If you don't see the formula, you're installation of brew might be
> out-of-date. Just execute `brew update` and try again.

### Quick start script example
Here's a really simple web application. Create a file called `app.groovy`:

```groovy
@RestController
class ThisWillActuallyRun {

	@RequestMapping("/")
	String home() {
		return "Hello World!"
	}

}
```

Then run it from a shell:

```
$ spring run app.groovy
```

> **Note:** It will take some time when you first run the application as dependencies
> are downloaded, subsequent runs will be much quicker.

Open [http://localhost:8080](http://localhost:8080) in your favorite web browser and you
should see  the following output:
> Hello World!

## Testing Your Code

The Spring Boot CLI has a `test` command. Example usage:

```
$ spring test app.groovy tests.groovy
Total: 1, Success: 1, : Failures: 0
Passed? true
```

Where `tests.groovy` contains JUnit `@Test` methods or Spock
`Specification` classes. All the common framework annotations and
static methods should be available to you without having to import
them. Example with JUnit (for the above application):

```groovy
class ApplicationTests {
    @Test
    void homeSaysHello() {
        assertEquals("Hello World", new ThisWillActuallyRun().home())
    }
}
```

You can add more tests by adding additional
files, or you might prefer to put them in a special directory.

## Applications with Multiple Source Files

You can use shell globbing to pick up multiple files in a single
directory, e.g.

```
$ spring run *.groovy
```

and this enables you to easily segregate your test or spec code from
the main application code, if that's what you prefer, e.g.

```
$ spring test app/*.groovy test/*.groovy
```

## Beans DSL

Spring has native support for a `beans{}` DSL (borrowed from
[Grails](http://grails.org)), and you can embedd bean definitions in
your Groovy application scripts using the same format. This is
sometimes a good way to include external features like middleware
declarations. E.g.

```groovy
@Configuration
class Application implements CommandLineRunner {

  @Autowired
  SharedService service
  
  @Override
  void run(String... args) {
    println service.message
  }

}

import my.company.SharedService

beans {
    service(SharedService) {
        message "Hello World"
    }
}
```

You can mix class declarations with `beans{}` in the same file as long
as they stay at the top level, or you can put the beans DSL in a
separate file if you prefer.

## Commandline Completion

Spring Boot CLI ships with a script that provides command completion
in a standard bash-like shell. You can source the script (also named
`spring`) in any shell, or put it in your personal or system-wide bash
completion initialization.  On a Debian system the system-wide scripts
are in `/etc/bash_completion.d` and all scripts in that directory are
executed in a new shell.  To run the script manually, e.g. if you have
installed using GVM

```
$ . ~/.gvm/springboot/current/bash_completion.d/spring
$ spring <HIT TAB HERE>
clean    -d       debug    help     run      test     version
```
