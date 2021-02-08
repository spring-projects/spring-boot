import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::image-name[]
tasks.getByName<BootBuildImage>("bootBuildImage") {
	imageName = "example.com/library/${project.name}"
}
// end::image-name[]

tasks.register("bootBuildImageName") {
    doFirst {
        println(tasks.getByName<BootBuildImage>("bootBuildImage").imageName)
    }
}
