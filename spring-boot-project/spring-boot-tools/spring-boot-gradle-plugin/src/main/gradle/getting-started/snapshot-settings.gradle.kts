pluginManagement {
	repositories {
		maven {
			url = uri("https://repo.spring.io/libs-snapshot")
		}
	}
	resolutionStrategy {
		eachPlugin {
			if (requested.id.id == "org.springframework.boot") {
				useModule("org.springframework.boot:spring-boot-gradle-plugin:${requested.version}")
			}
		}
	}
}
