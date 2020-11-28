import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::main-class[]
tasks.getByName<BootJar>("bootJar") {
	mainClass.set("com.example.ExampleApplication")
}
// end::main-class[]
