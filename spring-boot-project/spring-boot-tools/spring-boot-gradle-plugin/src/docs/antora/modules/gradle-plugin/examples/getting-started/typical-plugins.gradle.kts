// tag::apply[]
plugins {
	java
	id("org.springframework.boot") version "{version-spring-boot}"
}

apply(plugin = "io.spring.dependency-management")
// end::apply[]

tasks.register("verify") {
	doLast {
		project.plugins.getPlugin(JavaPlugin::class)
		project.plugins.getPlugin(io.spring.gradle.dependencymanagement.DependencyManagementPlugin::class)
	}
}
