plugins {
	java
	id("org.springframework.boot") version "{version-spring-boot}"
}

// tag::configure-platform[]
dependencies {
	implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
}
// end::configure-platform[]

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
}

repositories {
	maven {
		url = uri("repository")
	}
}

configurations.all {
	resolutionStrategy {
		eachDependency {
			if (requested.group == "org.springframework.boot") {
				useVersion("TEST-SNAPSHOT")
			}
		}
	}
}
