import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{version}"
}

tasks.getByName<BootJar>("bootJar") {
	mainClassName = "com.example.ExampleApplication"
}

// tag::launch-script-properties[]
tasks.getByName<BootJar>("bootJar") {
	launchScript {
		properties(mapOf("logFilename" to "example-app.log"))
	}
}
// end::launch-script-properties[]
