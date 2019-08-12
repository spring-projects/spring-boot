import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

plugins {
	java
	id("org.springframework.boot") version "{version}"
}

// tag::configure-bom[]
apply(plugin = "io.spring.dependency-management")

the<DependencyManagementExtension>().apply {
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
		url = uri("file:repository")
	}
}
