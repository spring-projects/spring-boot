import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

// tag::configure-bom[]
plugins {
	java
	id("org.springframework.boot") version "{version-spring-boot}" apply false
	id("io.spring.dependency-management") version "{version-dependency-management-plugin}"
}

dependencyManagement {
	imports {
		mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
	}
}
// end::configure-bom[]

the<DependencyManagementExtension>().apply {
	resolutionStrategy {
		eachDependency {
			if (requested.group == "org.springframework.boot") {
				useVersion("TEST-SNAPSHOT")
			}
		}
	}
}

repositories {
	maven {
		url = uri("repository")
	}
}
