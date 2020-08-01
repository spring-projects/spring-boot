import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

tasks.getByName<BootJar>("bootJar") {
	mainClassName = "com.example.ExampleApplication"
}

// tag::env[]
tasks.getByName<BootBuildImage>("bootBuildImage") {
	environment = ["BP_JVM_VERSION" : "13.0.1"]
}
// end::env[]
