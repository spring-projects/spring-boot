import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

plugins {
	id("org.springframework.boot") version "{version-spring-boot}"
}

apply(plugin = "io.spring.dependency-management")

// tag::custom-version[]
extra["slf4j.version"] = "1.7.20"
// end::custom-version[]

repositories {
	maven {
		url = uri("repository")
	}
}

the<DependencyManagementExtension>().apply {
	resolutionStrategy {
		eachDependency {
			if (requested.group == "org.springframework.boot") {
				useVersion("TEST-SNAPSHOT")
			}
		}
	}
}

tasks.register("slf4jVersion") {
	doLast {
		println(project.the<DependencyManagementExtension>().managedVersions["org.slf4j:slf4j-api"])
	}
}
