import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
	java
	id("org.springframework.boot") version "{version}"
}

// tag::main[]
tasks.getByName<BootRun>("bootRun") {
	main = "com.example.ExampleApplication"
}
// end::main[]

task("configuredMainClass") {
	doLast {
		println(tasks.getByName<BootRun>("bootRun").main)
	}
}
