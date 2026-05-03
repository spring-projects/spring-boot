plugins {
	java
	id("org.springframework.boot") version "{version-spring-boot}"
}

// tag::exclude-time[]
springBoot {
	buildInfo {
		excludes.set(setOf("time"))
	}
}
// end::exclude-time[]
