plugins {
	java
	application
	id("org.springframework.boot") version "{version}"
}

// tag::main-class[]
application {
	mainClassName = "com.example.ExampleApplication"
}
// end::main-class[]
