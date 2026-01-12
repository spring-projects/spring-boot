/*
 * Copyright 2012-present the original author or authors.
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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

/**
 * Extension to add {@code springRepositoryTransformers} utility methods.
 *
 * @author Phillip Webb
 */
public class RepositoryTransformersExtension {

	private static final String CREDENTIALS_MARKER = "{spring.mavenCredentials}";

	private static final String REPOSITORIES_MARKER = "{spring.mavenRepositories}";

	private static final String PLUGIN_REPOSITORIES_MARKER = "{spring.mavenPluginRepositories}";

	private final Project project;

	@Inject
	public RepositoryTransformersExtension(Project project) {
		this.project = project;
	}

	public Transformer<String, String> ant() {
		return this::transformAnt;
	}

	private String transformAnt(String line) {
		if (line.contains(REPOSITORIES_MARKER)) {
			return transform(line, (repository, indent) -> {
				String name = repository.getName();
				URI url = repository.getUrl();
				return "%s<ibiblio name=\"%s\" m2compatible=\"true\" root=\"%s\" />".formatted(indent, name, url);
			});
		}
		if (line.contains(CREDENTIALS_MARKER)) {
			Map<String, MavenCredential> hostCredentials = new LinkedHashMap<>();
			getSpringRepositories().forEach((repository) -> {
				if (repository.getName().startsWith("spring-commercial-")) {
					String host = repository.getUrl().getHost();
					hostCredentials.put(host,
							new MavenCredential("${env.COMMERCIAL_REPO_USERNAME}", "${env.COMMERCIAL_REPO_PASSWORD"));
				}
			});
			return transform(line, hostCredentials.entrySet(), (entry,
					indent) -> "%s<credentials host=\"%s\" realm=\"Artifactory Realm\" username=\"%s\" passwd=\"%s\" />%n"
						.formatted(indent, entry.getKey(), entry.getValue().username(), entry.getValue().password()));
		}
		return line;
	}

	public Transformer<String, String> mavenSettings() {
		return this::transformMavenSettings;
	}

	private String transformMavenSettings(String line) {
		if (line.contains(REPOSITORIES_MARKER)) {
			return transformMavenRepositories(line, false);
		}
		if (line.contains(PLUGIN_REPOSITORIES_MARKER)) {
			return transformMavenRepositories(line, true);
		}
		return line;
	}

	private String transformMavenRepositories(String line, boolean pluginRepository) {
		return transform(line, (repository, indent) -> mavenRepositoryXml(indent, repository, pluginRepository));
	}

	private String mavenRepositoryXml(String indent, MavenArtifactRepository repository, boolean pluginRepository) {
		String rootTag = pluginRepository ? "pluginRepository" : "repository";
		boolean snapshots = repository.getName().endsWith("-snapshot");
		StringBuilder xml = new StringBuilder();
		xml.append("%s<%s>%n".formatted(indent, rootTag));
		xml.append("%s\t<id>%s</id>%n".formatted(indent, repository.getName()));
		xml.append("%s\t<url>%s</url>%n".formatted(indent, repository.getUrl()));
		xml.append("%s\t<releases>%n".formatted(indent));
		xml.append("%s\t\t<enabled>%s</enabled>%n".formatted(indent, !snapshots));
		xml.append("%s\t</releases>%n".formatted(indent));
		xml.append("%s\t<snapshots>%n".formatted(indent));
		xml.append("%s\t\t<enabled>%s</enabled>%n".formatted(indent, snapshots));
		xml.append("%s\t</snapshots>%n".formatted(indent));
		xml.append("%s</%s>".formatted(indent, rootTag));
		return xml.toString();
	}

	private String transform(String line, BiFunction<MavenArtifactRepository, String, String> generator) {
		return transform(line, getSpringRepositories(), generator);
	}

	private <T> String transform(String line, Iterable<T> iterable, BiFunction<T, String, String> generator) {
		StringBuilder result = new StringBuilder();
		String indent = getIndent(line);
		iterable.forEach((item) -> {
			String fragment = generator.apply(item, indent);
			if (fragment != null) {
				result.append(!result.isEmpty() ? "\n" : "");
				result.append(fragment);
			}
		});
		return result.toString();
	}

	private List<MavenArtifactRepository> getSpringRepositories() {
		List<MavenArtifactRepository> springRepositories = new ArrayList<>(this.project.getRepositories()
			.withType(MavenArtifactRepository.class)
			.stream()
			.filter(this::isSpringRepository)
			.toList());
		Function<MavenArtifactRepository, Boolean> bySnapshots = (repository) -> repository.getName()
			.contains("snapshot");
		Function<MavenArtifactRepository, String> byName = MavenArtifactRepository::getName;
		Collections.sort(springRepositories, Comparator.comparing(bySnapshots).thenComparing(byName));
		return springRepositories;
	}

	private boolean isSpringRepository(MavenArtifactRepository repository) {
		return (repository.getName().startsWith("spring-"));
	}

	private String getIndent(String line) {
		return line.substring(0, line.length() - line.stripLeading().length());
	}

	static void apply(Project project) {
		project.getExtensions().create("springRepositoryTransformers", RepositoryTransformersExtension.class, project);
	}

	record MavenCredential(String username, String password) {

	}

}
