import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{version-spring-boot}"
}

// tag::buildpacks[]
tasks.named<BootBuildImage>("bootBuildImage") {
	buildpacks.set(listOf("file:///path/to/example-buildpack.tgz", "urn:cnb:builder:paketo-buildpacks/java"))
}
// end::buildpacks[]

tasks.register("bootBuildImageBuildpacks") {
	doFirst {
		for(reference in tasks.getByName<BootBuildImage>("bootBuildImage").buildpacks.get()) {
			print(reference)
		}
	}
}
