// tag::apply[]
plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

apply(plugin = "io.spring.dependency-management")
// end::apply[]

task("verify") {
	doLast {
		project.plugins.getPlugin(JavaPlugin::class)
		project.plugins.getPlugin(io.spring.gradle.dependencymanagement.DependencyManagementPlugin::class)
	}
}
