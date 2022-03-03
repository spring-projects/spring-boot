import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

tasks.named<BootJar>("bootJar") {
	mainClass.set("com.example.ExampleApplication")
}

// tag::include-launch-script[]
tasks.named<BootJar>("bootJar") {
	launchScript()
}
// end::include-launch-script[]
