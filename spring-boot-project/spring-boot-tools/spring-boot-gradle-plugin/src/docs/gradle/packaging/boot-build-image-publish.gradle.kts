import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

tasks.getByName<BootJar>("bootJar") {
	mainClassName = "com.example.ExampleApplication"
}

// tag::publish[]
tasks.getByName<BootBuildImage>("bootBuildImage") {
	imageName = "docker.example.com/library/${project.name}"
	publish = true
	docker {
		publishRegistry {
			username = "user"
			password = "secret"
			url = "https://docker.example.com/v1/"
			email = "user@example.com"
		}
	}
}
// end::publish[]
