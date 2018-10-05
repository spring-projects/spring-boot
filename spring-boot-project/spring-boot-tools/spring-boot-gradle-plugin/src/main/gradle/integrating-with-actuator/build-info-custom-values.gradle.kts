plugins {
	java
	id("org.springframework.boot") version "{version}"
}

// tag::custom-values[]
springBoot {
	buildInfo {
		properties {
			artifact = "example-app"
			version = "1.2.3"
			group = "com.example"
			name = "Example application"
		}
	}
}
// end::custom-values[]
