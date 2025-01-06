import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
	java
	id("org.springframework.boot") version "{version-spring-boot}"
}

// tag::main[]
tasks.named<BootRun>("bootRun") {
	mainClass.set("com.example.ExampleApplication")
}
// end::main[]

tasks.register("configuredMainClass") {
	doLast {
		println(tasks.getByName<BootRun>("bootRun").mainClass.get())
	}
}
