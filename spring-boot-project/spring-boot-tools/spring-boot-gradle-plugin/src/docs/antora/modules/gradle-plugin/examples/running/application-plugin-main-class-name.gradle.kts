plugins {
	java
	application
	id("org.springframework.boot") version "{version-spring-boot}"
}

// tag::main-class[]
application {
	mainClass.set("com.example.ExampleApplication")
}
// end::main-class[]
