import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::env[]
tasks.named<BootBuildImage>("bootBuildImage") {
	environment = mapOf("BP_JVM_VERSION" to "8.*")
}
// end::env[]

tasks.register("bootBuildImageEnvironment") {
	doFirst {
		for((name, value) in tasks.getByName<BootBuildImage>("bootBuildImage").environment) {
			print(name + "=" + value)
		}
	}
}

