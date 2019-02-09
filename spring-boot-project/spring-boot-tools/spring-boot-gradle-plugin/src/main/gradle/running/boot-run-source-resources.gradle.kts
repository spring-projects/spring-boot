import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
	java
	id("org.springframework.boot") version "{version}"
}

// tag::source-resources[]
tasks.getByName<BootRun>("bootRun") {
	sourceResources(sourceSets["main"])
}
// end::source-resources[]

task("configuredClasspath") {
	doLast {
		println(tasks.getByName<BootRun>("bootRun").classpath.files)
	}
}
