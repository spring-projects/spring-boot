import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

tasks.named<BootJar>("bootJar") {
	mainClass.set("com.example.ExampleApplication")
}

// tag::docker-auth-token[]
tasks.named<BootBuildImage>("bootBuildImage") {
	docker {
		builderRegistry {
			token = "9cbaf023786cd7..."
		}
	}
}
// end::docker-auth-token[]

tasks.register("bootBuildImageDocker") {
	doFirst {
		println("token=${tasks.getByName<BootBuildImage>("bootBuildImage").docker.builderRegistry.token}")
	}
}
