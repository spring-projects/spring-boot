import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::caches[]
tasks.named<BootBuildImage>("bootBuildImage") {
	buildCache {
		volume {
			name = "cache-${rootProject.name}.build"
		}
	}
	launchCache {
		volume {
			name = "cache-${rootProject.name}.launch"
		}
	}
}
// end::caches[]

tasks.register("bootBuildImageCaches") {
	doFirst {
		println("buildCache=" + tasks.getByName<BootBuildImage>("bootBuildImage").buildCache.asCache().volume.name)
		println("launchCache=" + tasks.getByName<BootBuildImage>("bootBuildImage").launchCache.asCache().volume.name)
	}
}
