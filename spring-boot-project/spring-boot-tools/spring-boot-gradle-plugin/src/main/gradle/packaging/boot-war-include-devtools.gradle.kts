import org.springframework.boot.gradle.tasks.bundling.BootWar

plugins {
	war
	id("org.springframework.boot") version "{version}"
}

tasks.getByName<BootWar>("bootWar") {
	mainClassName = "com.example.ExampleApplication"
	classpath(file("spring-boot-devtools-1.2.3.RELEASE.jar"))
}

// tag::include-devtools[]
tasks.getByName<BootWar>("bootWar") {
	isExcludeDevtools = false
}
// end::include-devtools[]
