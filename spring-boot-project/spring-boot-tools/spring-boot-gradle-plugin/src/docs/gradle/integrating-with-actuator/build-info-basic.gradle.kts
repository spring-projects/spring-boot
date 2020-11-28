plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::build-info[]
springBoot {
	buildInfo()
}
// end::build-info[]
