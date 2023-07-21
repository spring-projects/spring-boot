plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

dependencies {
	implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
	implementation("org.slf4j:slf4j-api")
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

// tag::custom-version[]
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.slf4j") {
            useVersion("1.7.20")
        }
    }
}
// end::custom-version[]
