plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::exclude-time[]
springBoot {
	buildInfo {
		excludes.set(setOf("time"))
	}
}
// end::exclude-time[]
