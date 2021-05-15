import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
	java
	id("org.springframework.boot") version "{version}"
}

// tag::system-property[]
tasks.getByName<BootRun>("bootRun") {
	systemProperty("com.example.property", findProperty("example") ?: "default")
}
// end::system-property[]

task("configuredSystemProperties") {
	doLast {
		tasks.getByName<BootRun>("bootRun").systemProperties.forEach { k, v -> 
			println("$k = $v")
		}
	}
}
