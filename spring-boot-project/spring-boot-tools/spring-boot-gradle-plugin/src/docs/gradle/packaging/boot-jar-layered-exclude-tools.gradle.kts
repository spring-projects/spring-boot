import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

tasks.getByName<BootJar>("bootJar") {
	mainClass.set("com.example.ExampleApplication")
}

// tag::layered[]
tasks.getByName<BootJar>("bootJar") {
	layered {
		isIncludeLayerTools = false
	}
}
// end::layered[]
