import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

tasks.getByName<BootJar>("bootJar") {
	mainClassName = "com.example.ExampleApplication"
}

// tag::builder[]
tasks.getByName<BootBuildImage>("bootBuildImage") {
	builder = "mine/java-cnb-builder"
}
// end::builder[]
