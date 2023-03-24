import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

tasks.named<BootJar>("bootJar") {
	mainClass.set("com.example.ExampleApplication")
}

// tag::docker-auth-user[]
tasks.named<BootBuildImage>("bootBuildImage") {
	docker {
		builderRegistry {
			username.set("user")
			password.set("secret")
			url.set("https://docker.example.com/v1/")
			email.set("user@example.com")
		}
	}
}
// end::docker-auth-user[]

tasks.register("bootBuildImageDocker") {
	doFirst {
		println("username=${tasks.getByName<BootBuildImage>("bootBuildImage").docker.builderRegistry.username.get()}")
		println("password=${tasks.getByName<BootBuildImage>("bootBuildImage").docker.builderRegistry.password.get()}")
		println("url=${tasks.getByName<BootBuildImage>("bootBuildImage").docker.builderRegistry.url.get()}")
		println("email=${tasks.getByName<BootBuildImage>("bootBuildImage").docker.builderRegistry.email.get()}")
	}
}
