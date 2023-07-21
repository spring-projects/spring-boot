import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::image-name[]
tasks.named<BootBuildImage>("bootBuildImage") {
	imageName.set("example.com/library/${project.name}")
}
// end::image-name[]

tasks.register("bootBuildImageName") {
	doFirst {
		println(tasks.getByName<BootBuildImage>("bootBuildImage").imageName.get())
	}
}
