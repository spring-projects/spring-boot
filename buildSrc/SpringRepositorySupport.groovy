/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//
// This script can be used in the `pluginManagement` block of a `settings.gradle` file to provide
// support for spring maven repositories.
//
// To use the script add the following as the first line in the `pluginManagement` block:
//
//     evaluate(new File("${rootDir}/buildSrc/SpringRepositorySupport.groovy")).apply(this)
//
// You can then use `spring.mavenRepositories()` to add the Spring repositories required for the
// version being built.
//

import java.util.function.*

def apply(settings) {
	def version =  property(settings, 'version')
	def buildType = property(settings, 'spring.build-type')
	SpringRepositoriesExtension.addTo(settings.pluginManagement.repositories, version, buildType)
	settings.gradle.allprojects {
		SpringRepositoriesExtension.addTo(repositories, version, buildType)
	}
}

private def property(settings, name) {
	def value = null
	try {
		value = settings.gradle.parent?.rootProject?.findProperty(name)
	}
	catch (Exception ex) {
	}
	try {
		value = (value != null) ? value : settings.ext.find(name)
	}
	catch (Exception ex) {
	}
	value = (value != null) ? value : loadProperty(settings, name)
	return value
}

private def loadProperty(settings, name) {
	def scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
	new File(scriptDir, "../gradle.properties").withInputStream {
		def properties = new Properties()
		properties.load(it)
		return properties.get(name)
	}
}

return this

class SpringRepositoriesExtension {

	private final def repositories
	private final def version
	private final def buildType
	private final UnaryOperator<String> environment

	@javax.inject.Inject
	SpringRepositoriesExtension(repositories, version, buildType) {
		this(repositories, version, buildType, System::getenv)
	}

	SpringRepositoriesExtension(repositories, version, buildType, environment) {
		this.repositories = repositories
		this.version = version
		this.buildType = buildType
		this.environment = environment
	}

	def mavenRepositories() {
		addRepositories { }
	}

	def mavenRepositories(condition) {
		if (condition) addRepositories { }
	}

	def mavenRepositoriesExcludingBootGroup() {
		addRepositories { maven ->
			maven.content { content ->
				content.excludeGroup("org.springframework.boot")
			}
		}
	}

	private void addRepositories(action) {
		addCommercialRepository("release", false, "/spring-enterprise-maven-prod-local", action)
		if (this.version.contains("-")) {
			addOssRepository("milestone", false, "/milestone", action)
		}
		if (this.version.endsWith("-SNAPSHOT")) {
			addCommercialRepository("snapshot", true, "/spring-enterprise-maven-dev-local", action)
			addOssRepository("snapshot", true, "/snapshot", action)
		}
	}

	private void addOssRepository(id, snapshot, path, action) {
		def name = "spring-oss-" + id
		def url = "https://repo.spring.io" + path
		addRepository(name, snapshot, url, action)
	}

	private void addCommercialRepository(id, snapshot, path, action) {
		if (!"commercial".equalsIgnoreCase(this.buildType)) return
		def name = "spring-commercial-" + id
		def url = fromEnv("COMMERCIAL_%SREPO_URL", id, "https://usw1.packages.broadcom.com" + path)
		def username = fromEnv("COMMERCIAL_%SREPO_USERNAME", id)
		def password = fromEnv("COMMERCIAL_%SREPO_PASSWORD", id)
		addRepository(name, snapshot, url, { maven ->
			maven.credentials { credentials ->
				credentials.setUsername(username)
				credentials.setPassword(password)
			}
			action(maven)
		})
	}

	private void addRepository(name, snapshot, url, action) {
		this.repositories.maven { maven ->
			maven.setName(name)
			maven.setUrl(url)
			maven.mavenContent { mavenContent ->
				if (snapshot) {
					mavenContent.snapshotsOnly()
				} else {
					mavenContent.releasesOnly()
				}
			}
			action(maven)
		}
	}

	private String fromEnv(template, id) {
		return fromEnv(template, id, null)
	}

	private String fromEnv(template, id, defaultValue) {
		String value = this.environment.apply(template.formatted(id.toUpperCase() + "_"))
		value = (value != null) ? value : this.environment.apply(template.formatted(""))
		return (value != null) ? value : defaultValue
	}

	static def addTo(repositories, version, buildType) {
		repositories.extensions.create("spring", SpringRepositoriesExtension.class, repositories, version, buildType)
	}

}