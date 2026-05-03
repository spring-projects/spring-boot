import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{version-spring-boot}"
}

tasks.named<BootJar>("bootJar") {
	mainClass.set("com.example.ExampleApplication")
}

// tag::layered[]
tasks.named<BootJar>("bootJar") {
	layered {
		application {
			intoLayer("spring-boot-loader") {
				include("org/springframework/boot/loader/**")
			}
			intoLayer("application")
		}
		dependencies {
			intoLayer("application") {
				includeProjectDependencies()
			}
			intoLayer("snapshot-dependencies") {
				include("*:*:*SNAPSHOT")
			}
			intoLayer("dependencies")
		}
		layerOrder.set(listOf("dependencies", "spring-boot-loader", "snapshot-dependencies", "application"))
	}
}
// end::layered[]
