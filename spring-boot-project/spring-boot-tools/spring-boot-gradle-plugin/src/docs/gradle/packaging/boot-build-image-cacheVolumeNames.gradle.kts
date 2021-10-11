import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::cacheVolumeNames[]
tasks.getByName<BootBuildImage>("bootBuildImage") {
	cacheVolumeNames = mapOf("build" to "example-build-cachevol",
						"launch" to "example-launch-cachevol")
}
// end::cacheVolumeNames[]

tasks.register("bootBuildImageEnvironment") {
	doFirst {
		for((type, name) in tasks.getByName<BootBuildImage>("bootBuildImage").cacheVolumeNames) {
			print(type + "=" + name)
		}
	}
}
