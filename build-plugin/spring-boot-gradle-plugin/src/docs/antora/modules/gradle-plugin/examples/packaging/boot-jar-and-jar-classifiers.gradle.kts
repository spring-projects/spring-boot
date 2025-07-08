import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{version-spring-boot}"
}

// tag::classifiers[]
tasks.named<BootJar>("bootJar") {
	archiveClassifier.set("boot")
}

tasks.named<Jar>("jar") {
	archiveClassifier.set("")
}
// end::classifiers[]

tasks.named<BootJar>("bootJar") {
	mainClass.set("com.example.Application")
}
