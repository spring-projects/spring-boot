import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
	java
	id("org.springframework.boot") version "{version-spring-boot}"
}

// tag::source-resources[]
tasks.named<BootRun>("bootRun") {
	sourceResources(sourceSets["main"])
}
// end::source-resources[]

tasks.register("configuredClasspath") {
	doLast {
		println(tasks.getByName<BootRun>("bootRun").classpath.files)
	}
}
