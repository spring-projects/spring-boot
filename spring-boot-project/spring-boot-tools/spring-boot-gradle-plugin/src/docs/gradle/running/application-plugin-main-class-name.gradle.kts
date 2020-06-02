import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
	java
	application
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::main-class[]
application {
	mainClassName = "com.example.ExampleApplication"
}
// end::main-class[]

task("configuredMainClass") {
	doLast {
		println(tasks.getByName<BootRun>("bootRun").main)
	}
}
