plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::custom-values[]
springBoot {
	buildInfo {
		properties {
			artifact.set("example-app")
			version.set("1.2.3")
			group.set("com.example")
			name.set("Example application")
		}
	}
}
// end::custom-values[]
