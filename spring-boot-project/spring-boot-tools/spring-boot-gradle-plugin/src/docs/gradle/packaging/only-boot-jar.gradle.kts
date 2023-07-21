import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::disable-jar[]
tasks.named<Jar>("jar") {
	enabled = false
}
// end::disable-jar[]

tasks.named<BootJar>("bootJar") {
	mainClass.set("com.example.Application")
}
