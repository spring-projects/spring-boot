import org.springframework.boot.gradle.tasks.bundling.BootWar

plugins {
	war
	id("org.springframework.boot") version "{version-spring-boot}"
}

tasks.named<BootWar>("bootWar") {
	mainClass.set("com.example.ExampleApplication")
}

dependencies {
	"developmentOnly"(files("spring-boot-devtools-1.2.3.RELEASE.jar"))
}

// tag::include-devtools[]
tasks.named<BootWar>("bootWar") {
	classpath(configurations["developmentOnly"])
}
// end::include-devtools[]
