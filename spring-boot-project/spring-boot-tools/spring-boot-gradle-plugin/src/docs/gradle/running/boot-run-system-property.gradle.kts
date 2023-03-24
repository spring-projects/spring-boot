import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
	java
	id("org.springframework.boot") version "{version}"
}

// tag::system-property[]
tasks.named<BootRun>("bootRun") {
	systemProperty("com.example.property", findProperty("example") ?: "default")
}
// end::system-property[]

tasks.register("configuredSystemProperties") {
	doLast {
		tasks.getByName<BootRun>("bootRun").systemProperties.forEach { k, v ->
			println("$k = $v")
		}
	}
}
