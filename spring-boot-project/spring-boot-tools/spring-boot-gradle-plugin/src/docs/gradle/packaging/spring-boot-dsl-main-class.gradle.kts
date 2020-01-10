plugins {
	war
	id("org.springframework.boot") version "{version}"
}

// tag::main-class[]
springBoot {
	mainClassName = "com.example.ExampleApplication"
}
// end::main-class[]
