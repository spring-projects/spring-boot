import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::buildpacks[]
tasks.named<BootBuildImage>("bootBuildImage") {
	buildpacks = listOf("file:///path/to/example-buildpack.tgz", "urn:cnb:builder:paketo-buildpacks/java")
}
// end::buildpacks[]

tasks.register("bootBuildImageBuildpacks") {
	doFirst {
		for(reference in tasks.getByName<BootBuildImage>("bootBuildImage").buildpacks) {
			print(reference)
		}
	}
}
