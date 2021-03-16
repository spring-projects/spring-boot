import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::disable-jar[]
tasks.getByName<Jar>("jar") {
	enabled = false
}
// end::disable-jar[]

tasks.getByName<BootJar>("bootJar") {
	mainClass.set("com.example.Application")
}
