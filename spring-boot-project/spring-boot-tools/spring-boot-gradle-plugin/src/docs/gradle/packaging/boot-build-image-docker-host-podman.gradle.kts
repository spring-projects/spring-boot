import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

tasks.named<BootJar>("bootJar") {
	mainClass.set("com.example.ExampleApplication")
}

// tag::docker-host[]
tasks.named<BootBuildImage>("bootBuildImage") {
	docker {
		host.set("unix:///run/user/1000/podman/podman.sock")
		bindHostToBuilder.set(true)
	}
}
// end::docker-host[]

tasks.register("bootBuildImageDocker") {
	doFirst {
		println("host=${tasks.getByName<BootBuildImage>("bootBuildImage").docker.host.get()}")
		println("bindHostToBuilder=${tasks.getByName<BootBuildImage>("bootBuildImage").docker.bindHostToBuilder.get()}")
	}
}
