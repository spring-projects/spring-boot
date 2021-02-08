import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

plugins {
	id("org.springframework.boot") version "{gradle-project-version}"
}

apply(plugin = "io.spring.dependency-management")

// tag::custom-version[]
extra["slf4j.version"] = "1.7.20"
// end::custom-version[]

repositories {
	maven {
		url = uri("file:repository")
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

task("slf4jVersion") {
	doLast {
		println(project.the<DependencyManagementExtension>().managedVersions["org.slf4j:slf4j-api"])
	}
}
