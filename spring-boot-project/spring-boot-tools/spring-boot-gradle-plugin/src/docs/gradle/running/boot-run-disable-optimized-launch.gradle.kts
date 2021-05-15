import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::launch[]
tasks.getByName<BootRun>("bootRun") {
	isOptimizedLaunch = false
}
// end::launch[]

task("optimizedLaunch") {
	doLast {
		println(tasks.getByName<BootRun>("bootRun").isOptimizedLaunch)
	}
}
