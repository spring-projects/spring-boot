import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{version}"
}

// tag::main-class[]
tasks.getByName<BootJar>("bootJar") {
	mainClassName = "com.example.ExampleApplication"
}
// end::main-class[]
