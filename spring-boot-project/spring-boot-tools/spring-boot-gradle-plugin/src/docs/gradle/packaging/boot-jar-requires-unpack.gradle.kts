import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

repositories {
	mavenCentral()
}

dependencies {
	runtimeOnly("org.jruby:jruby-complete:1.7.25")
}

tasks.named<BootJar>("bootJar") {
	mainClass.set("com.example.ExampleApplication")
}

// tag::requires-unpack[]
tasks.named<BootJar>("bootJar") {
	requiresUnpack("**/jruby-complete-*.jar")
}
// end::requires-unpack[]
