import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{version}"
}

// tag::layered[]
tasks.getByName<BootJar>("bootJar") {
	layered {
		application {
			intoLayer("application")
		}
		dependencies {
			intoLayer("snapshot-dependencies") {
				include("*:*:*SNAPSHOT")
			}
			intoLayer("dependencies") {
		}
		layersOrder("dependencies", "snapshot-dependencies", "application")
	}
}
// end::layered[]
