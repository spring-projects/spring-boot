import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

tasks.named<BootJar>("bootJar") {
	mainClass.set("com.example.ExampleApplication")
}

// tag::publish[]
tasks.named<BootBuildImage>("bootBuildImage") {
	imageName.set("docker.example.com/library/${project.name}")
	publish.set(true)
	docker {
		publishRegistry {
			username.set("user")
			password.set("secret")
		}
	}
}
// end::publish[]

tasks.register("bootBuildImagePublish") {
	doFirst {
		println(tasks.getByName<BootBuildImage>("bootBuildImage").publish.get())
	}
}
