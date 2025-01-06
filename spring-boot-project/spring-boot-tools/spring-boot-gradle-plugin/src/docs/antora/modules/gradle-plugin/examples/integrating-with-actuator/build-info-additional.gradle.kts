plugins {
	java
	id("org.springframework.boot") version "{version-spring-boot}"
}

// tag::additional[]
springBoot {
	buildInfo {
		properties {
			additional.set(mapOf(
				"a" to "alpha",
				"b" to "bravo"
			))
		}
	}
}
// end::additional[]

