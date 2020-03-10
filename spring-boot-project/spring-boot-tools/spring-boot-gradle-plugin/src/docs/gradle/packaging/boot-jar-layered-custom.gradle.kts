import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{version}"
}

// tag::layered[]
tasks.getByName<BootJar>("bootJar") {
	layers {
		includeLayerTools = false
		layers("dependencies", "snapshot-dependencies", "resources", "application")
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
		classes {
			layerContent("resources") {
				locations {
					include("META-INF/resources/**", "resources/**")
					include("static/**", "public/**")
				}
			}
			layerContent("application") {
				locations {
					include("**")
				}
			}
		}
	}
}
// end::layered[]
