import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{version-spring-boot}"
}

// tag::env-runtime[]
tasks.named<BootBuildImage>("bootBuildImage") {
	environment.putAll(mapOf(
		"BPE_DELIM_JAVA_TOOL_OPTIONS" to " ",
		"BPE_APPEND_JAVA_TOOL_OPTIONS" to "-XX:+HeapDumpOnOutOfMemoryError"
	))
}
// end::env-runtime[]

tasks.register("bootBuildImageEnvironment") {
	doFirst {
		for((name, value) in tasks.getByName<BootBuildImage>("bootBuildImage").environment.get()) {
			print(name + "=" + value)
		}
	}
}

