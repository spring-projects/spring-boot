import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::classifiers[]
tasks.getByName<BootJar>("bootJar") {
	classifier = "boot"
}

tasks.getByName<Jar>("jar") {
	classifier = ""
}
// end::classifiers[]

tasks.getByName<BootJar>("bootJar") {
	mainClass.set("com.example.Application")
}
