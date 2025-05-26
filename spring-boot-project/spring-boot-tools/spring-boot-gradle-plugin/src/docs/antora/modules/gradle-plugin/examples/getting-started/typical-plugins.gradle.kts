// tag::apply[]
plugins {
	java
	id("org.springframework.boot") version "{version-spring-boot}"
}

apply(plugin = "io.spring.dependency-management")
// end::apply[]

tasks.register("verify") {
	val plugins = project.plugins
	doLast {
		plugins.getPlugin(JavaPlugin::class)
		plugins.getPlugin(io.spring.gradle.dependencymanagement.DependencyManagementPlugin::class)
	}
}
