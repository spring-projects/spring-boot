import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

tasks.getByName<BootJar>("bootJar") {
	mainClassName = "com.example.ExampleApplication"
}

// tag::image-name[]
tasks.getByName<BootBuildImage>("bootBuildImage") {
	imageName = "example.com/library/${project.artifactId}"
}
// end::image-name[]
