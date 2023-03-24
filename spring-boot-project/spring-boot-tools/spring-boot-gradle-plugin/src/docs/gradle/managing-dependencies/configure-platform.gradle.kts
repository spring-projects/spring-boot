plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
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
		url = uri("file:repository")
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
