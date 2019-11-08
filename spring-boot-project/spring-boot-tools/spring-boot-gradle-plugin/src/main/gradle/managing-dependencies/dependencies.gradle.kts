plugins {
	java
	id("org.springframework.boot") version "{version}"
}

apply(plugin = "io.spring.dependency-management")

// tag::dependencies[]
dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}
// end::dependencies[]
