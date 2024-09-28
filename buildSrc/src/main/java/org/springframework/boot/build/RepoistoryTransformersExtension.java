/*
 * Copyright 2024-2024 the original author or authors.
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

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

/**
 * Extension to add {@code springRepoistoryTransformers} utility methods.
 *
 * @author Phillip Webb
 */
public class RepoistoryTransformersExtension {

	private static final String MARKER = "{spring.mavenRepositories}";

	private final Project project;

	@Inject
	public RepoistoryTransformersExtension(Project project) {
		this.project = project;
	}

	public Transformer<String, String> ant() {
		return this::transformAnt;
	}

	private String transformAnt(String line) {
		if (line.contains(MARKER)) {
			StringBuilder result = new StringBuilder();
			String indent = getIndent(line);
			this.project.getRepositories().withType(MavenArtifactRepository.class, (repository) -> {
				String name = repository.getName();
				if (name.startsWith("spring-")) {
					result.append(!result.isEmpty() ? "\n" : "");
					result.append("%s<ibiblio name=\"%s\" m2compatible=\"true\" root=\"%s\" />".formatted(indent, name,
							repository.getUrl()));
				}
			});
			return result.toString();
		}
		return line;
	}

	public Transformer<String, String> mavenSettings() {
		return this::transformMavenSettings;
	}

	private String transformMavenSettings(String line) {
		if (line.contains(MARKER)) {
			StringBuilder result = new StringBuilder();
			String indent = getIndent(line);
			this.project.getRepositories().withType(MavenArtifactRepository.class, (repository) -> {
				String name = repository.getName();
				if (name.startsWith("spring-")) {
					result.append(!result.isEmpty() ? "\n" : "");
					result.append(mavenRepositoryXml(indent, repository));
				}
			});
			return result.toString();
		}
		return line;
	}

	private String mavenRepositoryXml(String indent, MavenArtifactRepository repository) {
		boolean snapshots = repository.getName().endsWith("-snapshot");
		StringBuilder xml = new StringBuilder();
		xml.append("%s<repository>%n".formatted(indent));
		xml.append("%s\t<id>%s</id>%n".formatted(indent, repository.getName()));
		xml.append("%s\t<url>%s</url>%n".formatted(indent, repository.getUrl()));
		xml.append("%s\t<releases>%n".formatted(indent));
		xml.append("%s\t\t<enabled>%s</enabled>%n".formatted(indent, !snapshots));
		xml.append("%s\t</releases>%n".formatted(indent));
		xml.append("%s\t<snapshots>%n".formatted(indent));
		xml.append("%s\t\t<enabled>%s</enabled>%n".formatted(indent, snapshots));
		xml.append("%s\t</snapshots>%n".formatted(indent));
		xml.append("%s</repository>".formatted(indent));
		return xml.toString();
	}

	private String getIndent(String line) {
		return line.substring(0, line.length() - line.stripLeading().length());
	}

	static void apply(Project project) {
		project.getExtensions().create("springRepoistoryTransformers", RepoistoryTransformersExtension.class, project);
	}

}
