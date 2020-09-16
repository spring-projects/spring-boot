import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

tasks.getByName<BootJar>("bootJar") {
	mainClassName = "com.example.ExampleApplication"
}

// tag::docker-auth-user[]
tasks.getByName<BootBuildImage>("bootBuildImage") {
	docker {
		registry {
			username = "user"
			password = "secret"
			url = "https://docker.example.com/v1/"
			email = "user@example.com"
		}
	}
}
// end::docker-auth-user[]
