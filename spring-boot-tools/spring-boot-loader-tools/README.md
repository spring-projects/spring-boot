# Spring Boot - Loader Tools
The Spring Boot Loader Tools module provides support utilities to help when creating
[Spring Boot Loader](../spring-boot-loader/README.md) compatible archives. This module is
used by the various build system plugins that we provide.

> **Note:** The quickest way to build a compatible archive is to use the
> [spring-boot-maven-plugin](../spring-boot-maven-plugin/README.md) or
> [spring-boot-gradle-plugin](../spring-boot-gradle-plugin/README.md).

## Repackaging archives
To repackage an existing archive so that it becomes a self-contained executable archive
use `org.springframework.boot.loader.tools.Repackager`. The `Repackager` class takes a
single constructor argument that refers to an existing jar or war archive. Use one of the
two available `repackage()` methods to either replace the original file or write to a new
destination. Various settings can also be configured on the repackager before it is
run.

## Libraries
When repackaging an archive you can include references to dependency files using the
`org.springframework.boot.loader.tools.Libraries` interface. We don't provide any
concrete implementations of `Libraries` here as they are usually build system specific.

If your archive already includes libraries you can use `Libraries.NONE`

## Finding a main class
If you don't use `Repackager.setMainClass()` to specify a main class, the repackager will
use [ASM](http://asm.ow2.org/) to read class files and attempt to find a suitable class.
The first class with a `public static void main(String[] args)` method will be used.
Searching is performed using a breadth first algorithm, with the assumption that the main
class will appear high in the package structure.

## Example
Here is a typical example repackage:

```java
Repackager repackager = new Repackager(sourceJarFile);
repackager.setBackupSource(false);
repackager.repackage(new Libraries() {
			@Override
			public void doWithLibraries(LibraryCallback callback) throws IOException {
				// Build system specific implementation, callback for each dependency
				// callback.library(nestedFile, LibraryScope.COMPILE);
			}
		});

```

## Further Reading
For more information on how Spring Boot Loader archives work take a look at the
[spring-boot-loader](../spring-boot-loader/README.md) module. If you want to see how we use this
library the [Maven](../spring-boot-maven-plugin/README.md) and
[Gradle](../spring-boot-gradle-plugin/README.md) plugins are good place to start.
