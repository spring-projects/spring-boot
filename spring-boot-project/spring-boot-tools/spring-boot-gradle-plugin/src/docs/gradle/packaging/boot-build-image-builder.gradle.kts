import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

tasks.named<BootJar>("bootJar") {
	mainClass.set("com.example.ExampleApplication")
}

// tag::builder[]
tasks.named<BootBuildImage>("bootBuildImage") {
	builder = "mine/java-cnb-builder"
	runImage = "mine/java-cnb-run"
}
// end::builder[]

tasks.register("bootBuildImageBuilder") {
	doFirst {
		println("builder=${tasks.getByName<BootBuildImage>("bootBuildImage").builder}")
		println("runImage=${tasks.getByName<BootBuildImage>("bootBuildImage").runImage}")
	}
}
