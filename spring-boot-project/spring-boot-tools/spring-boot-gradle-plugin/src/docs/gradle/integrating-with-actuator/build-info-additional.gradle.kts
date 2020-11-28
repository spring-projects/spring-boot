plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::additional[]
springBoot {
	buildInfo {
		properties {
			additional = mapOf(
				"a" to "alpha",
				"b" to "bravo"
			)
		}
	}
}
// end::additional[]

