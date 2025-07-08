import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{version-spring-boot}"
}

// tag::env[]
tasks.named<BootBuildImage>("bootBuildImage") {
	environment.putAll(mapOf("HTTP_PROXY" to "http://proxy.example.com",
						"HTTPS_PROXY" to "https://proxy.example.com"))
}
// end::env[]

tasks.register("bootBuildImageEnvironment") {
	doFirst {
		for((name, value) in tasks.getByName<BootBuildImage>("bootBuildImage").environment.get()) {
			print(name + "=" + value)
		}
	}
}
