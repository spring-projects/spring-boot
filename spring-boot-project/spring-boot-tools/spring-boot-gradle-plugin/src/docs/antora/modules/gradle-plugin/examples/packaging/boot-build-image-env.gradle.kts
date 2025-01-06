import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{version-spring-boot}"
}

// tag::env[]
tasks.named<BootBuildImage>("bootBuildImage") {
	environment.put("BP_JVM_VERSION", "17")
}
// end::env[]

tasks.register("bootBuildImageEnvironment") {
	doFirst {
		for((name, value) in tasks.getByName<BootBuildImage>("bootBuildImage").environment.get()) {
			print(name + "=" + value)
		}
	}
}

