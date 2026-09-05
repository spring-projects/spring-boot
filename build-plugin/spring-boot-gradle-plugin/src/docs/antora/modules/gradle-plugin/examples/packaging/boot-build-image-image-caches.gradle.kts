import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{version-spring-boot}"
}

// tag::caches[]
tasks.named<BootBuildImage>("bootBuildImage") {
	buildCache {
		image {
			name.set("docker.io/library/${rootProject.name}:build")
		}
	}
}
// end::caches[]

tasks.register("bootBuildImageCaches") {
	doFirst {
		println("buildCache=" + tasks.getByName<BootBuildImage>("bootBuildImage").buildCache.asCache()?.image?.name)
	}
}
