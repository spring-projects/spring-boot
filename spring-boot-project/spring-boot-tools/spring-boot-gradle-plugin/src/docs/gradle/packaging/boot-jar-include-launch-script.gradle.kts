import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

tasks.getByName<BootJar>("bootJar") {
	mainClassName = "com.example.ExampleApplication"
}

// tag::include-launch-script[]
tasks.getByName<BootJar>("bootJar") {
	launchScript()
}
// end::include-launch-script[]
