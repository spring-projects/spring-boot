/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.build;

import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.gradle.api.Project;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPomDeveloperSpec;
import org.gradle.api.publish.maven.MavenPomIssueManagement;
import org.gradle.api.publish.maven.MavenPomLicenseSpec;
import org.gradle.api.publish.maven.MavenPomOrganization;
import org.gradle.api.publish.maven.MavenPomScm;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

/**
 * Conventions that are applied in the presence of the {@link MavenPublishPlugin}. When
 * the plugin is applied:
 *
 * <ul>
 * <li>If the {@code deploymentRepository} property has been set, a
 * {@link MavenArtifactRepository Maven artifact repository} is configured to publish to
 * it.
 * <li>The poms of all {@link MavenPublication Maven publications} are customized to meet
 * Maven Central's requirements.
 * <li>If the {@link JavaPlugin Java plugin} has also been applied:
 * <ul>
 * <li>Creation of Javadoc and source jars is enabled.
 * <li>Publication metadata (poms and Gradle module metadata) is configured to use
 * resolved versions.
 * </ul>
 * </ul>
 *
 * <p/>
 *
 * @author Andy Wilkinson
 * @author Christoph Dreis
 * @author Mike Smithson
 */
class MavenPublishingConventions {

	void apply(Project project) {
		project.getPlugins().withType(MavenPublishPlugin.class).all((mavenPublish) -> {
			PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
			if (project.hasProperty("deploymentRepository")) {
				publishing.getRepositories().maven((mavenRepository) -> {
					mavenRepository.setUrl(project.property("deploymentRepository"));
					mavenRepository.setName("deployment");
				});
			}
			publishing.getPublications().withType(MavenPublication.class)
					.all((mavenPublication) -> customizeMavenPublication(mavenPublication, project));
			project.getPlugins().withType(JavaPlugin.class).all((javaPlugin) -> {
				JavaPluginExtension extension = project.getExtensions().getByType(JavaPluginExtension.class);
				extension.withJavadocJar();
				extension.withSourcesJar();
			});
		});
	}

	private void customizeMavenPublication(MavenPublication publication, Project project) {
		customizePom(publication.getPom(), project);
		project.getPlugins().withType(JavaPlugin.class)
				.all((javaPlugin) -> customizeJavaMavenPublication(publication, project));
	}

	private void customizePom(MavenPom pom, Project project) {
		pom.getUrl().set("https://spring.io/projects/spring-boot");
		pom.getName().set(project.provider(project::getName));
		pom.getDescription().set(project.provider(project::getDescription));
		if (!isUserInherited(project)) {
			pom.organization(this::customizeOrganization);
		}
		pom.licenses(this::customizeLicences);
		pom.developers(this::customizeDevelopers);
		pom.scm((scm) -> customizeScm(scm, project));
		if (!isUserInherited(project)) {
			pom.issueManagement(this::customizeIssueManagement);
		}
	}

	private void customizeJavaMavenPublication(MavenPublication publication, Project project) {
		publication.versionMapping((strategy) -> strategy.usage(Usage.JAVA_API, (mappingStrategy) -> mappingStrategy
				.fromResolutionOf(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)));
		publication.versionMapping((strategy) -> strategy.usage(Usage.JAVA_RUNTIME,
				(mappingStrategy) -> mappingStrategy.fromResolutionResult()));
	}

	private void customizeOrganization(MavenPomOrganization organization) {
		organization.getName().set("Pivotal Software, Inc.");
		organization.getUrl().set("https://spring.io");
	}

	private void customizeLicences(MavenPomLicenseSpec licences) {
		licences.license((licence) -> {
			licence.getName().set("Apache License, Version 2.0");
			licence.getUrl().set("https://www.apache.org/licenses/LICENSE-2.0");
		});
	}

	private void customizeDevelopers(MavenPomDeveloperSpec developers) {
		developers.developer((developer) -> {
			developer.getName().set("Pivotal");
			developer.getEmail().set("info@pivotal.io");
			developer.getOrganization().set("Pivotal Software, Inc.");
			developer.getOrganizationUrl().set("https://www.spring.io");
		});
	}

	private void customizeScm(MavenPomScm scm, Project project) {
		if (!isUserInherited(project)) {
			scm.getConnection().set("scm:git:git://github.com/spring-projects/spring-boot.git");
			scm.getDeveloperConnection().set("scm:git:ssh://git@github.com/spring-projects/spring-boot.git");
		}
		scm.getUrl().set("https://github.com/spring-projects/spring-boot");
	}

	private void customizeIssueManagement(MavenPomIssueManagement issueManagement) {
		issueManagement.getSystem().set("GitHub");
		issueManagement.getUrl().set("https://github.com/spring-projects/spring-boot/issues");
	}

	private boolean isUserInherited(Project project) {
		return "spring-boot-starter-parent".equals(project.getName())
				|| "spring-boot-dependencies".equals(project.getName());
	}

}
