import org.springframework.boot.gradle.tasks.bundling.BootWar

plugins {
	war
	id("org.springframework.boot") version "{gradle-project-version}"
}

tasks.named<BootWar>("bootWar") {
	mainClass.set("com.example.ExampleApplication")
}

// tag::properties-launcher[]
tasks.named<BootWar>("bootWar") {
	manifest {
		attributes("Main-Class" to "org.springframework.boot.loader.PropertiesLauncher")
	}
}
// end::properties-launcher[]
