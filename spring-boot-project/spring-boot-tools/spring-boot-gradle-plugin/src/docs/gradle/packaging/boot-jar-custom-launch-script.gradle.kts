import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

tasks.getByName<BootJar>("bootJar") {
	mainClass.set("com.example.ExampleApplication")
}

// tag::custom-launch-script[]
tasks.getByName<BootJar>("bootJar") {
	launchScript {
		script = file("src/custom.script")
	}
}
// end::custom-launch-script[]
