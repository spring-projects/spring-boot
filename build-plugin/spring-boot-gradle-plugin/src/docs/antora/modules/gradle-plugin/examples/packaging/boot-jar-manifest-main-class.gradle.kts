import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{version-spring-boot}"
}

// tag::main-class[]
tasks.named<BootJar>("bootJar") {
	manifest {
		attributes("Start-Class" to "com.example.ExampleApplication")
	}
}
// end::main-class[]
