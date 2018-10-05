plugins {
	war
	id("org.springframework.boot") version "{version}"
}

apply(plugin = "io.spring.dependency-management")

// tag::dependencies[]
dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	providedRuntime("org.springframework.boot:spring-boot-starter-tomcat")
}
// end::dependencies[]
