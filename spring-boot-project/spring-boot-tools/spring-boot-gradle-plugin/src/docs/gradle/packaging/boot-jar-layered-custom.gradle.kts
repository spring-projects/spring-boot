import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{version}"
}

// tag::layered[]
tasks.getByName<BootJar>("bootJar") {
	layers {
		layersOrder("dependencies", "snapshot-dependencies", "application")
		libraries {
			layerContent("snapshot-dependencies") {
				coordinates {
					include("*:*:*SNAPSHOT")
				}
			}
			layerContent("dependencies") {
				coordinates {
					include("*:*")
				}
			}
		}
		application {
			layerContent("application") {
				locations {
					include("**")
				}
			}
		}
	}
}
// end::layered[]
