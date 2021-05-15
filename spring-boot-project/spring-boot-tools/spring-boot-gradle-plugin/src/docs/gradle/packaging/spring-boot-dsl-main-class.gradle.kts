plugins {
	war
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::main-class[]
springBoot {
	mainClass.set("com.example.ExampleApplication")
}
// end::main-class[]
