plugins {
	java
	id("org.springframework.boot") version "{version-spring-boot}"
}

dependencies {
	implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
	implementation("org.slf4j:slf4j-api")
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

// tag::custom-version[]
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.slf4j") {
            useVersion("1.7.20")
        }
    }
}
// end::custom-version[]
