plugins {
	java
	maven
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::upload[]
tasks.getByName<Upload>("uploadBootArchives") {
	repositories.withGroovyBuilder {
		"mavenDeployer" {
			"repository"("url" to "https://repo.example.com")
		}
	}
}
// end::upload[]

val url = tasks.getByName<Upload>("uploadBootArchives")
		.repositories
		.withGroovyBuilder { getProperty("mavenDeployer") }
		.withGroovyBuilder { getProperty("repository") }
		.withGroovyBuilder { getProperty("url") }
task("deployerRepository") {
	doLast {
		println(url)
	}
}
