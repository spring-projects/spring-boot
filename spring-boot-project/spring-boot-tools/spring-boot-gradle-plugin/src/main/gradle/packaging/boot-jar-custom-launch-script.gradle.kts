import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{version}"
}

tasks.getByName<BootJar>("bootJar") {
	mainClassName = "com.example.ExampleApplication"
}

// tag::custom-launch-script[]
tasks.getByName<BootJar>("bootJar") {
	launchScript {
		script = file("src/custom.script")
	}
}
// end::custom-launch-script[]
