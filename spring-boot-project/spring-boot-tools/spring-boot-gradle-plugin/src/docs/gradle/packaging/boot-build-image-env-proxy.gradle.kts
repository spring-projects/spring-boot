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
	environment = [
		"HTTP_PROXY" : "http://proxy.example.com",
		"HTTPS_PROXY" : "https://proxy.example.com"
	]
}
// end::env[]
