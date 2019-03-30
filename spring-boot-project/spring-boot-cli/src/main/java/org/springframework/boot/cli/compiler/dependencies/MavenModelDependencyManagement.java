/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.cli.compiler.dependencies;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;

import org.springframework.boot.cli.compiler.dependencies.Dependency.Exclusion;

/**
 * {@link DependencyManagement} derived from a Maven {@link Model}.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 */
public class MavenModelDependencyManagement implements DependencyManagement {

	private final List<Dependency> dependencies;

	private final Map<String, Dependency> byArtifactId = new LinkedHashMap<>();

	public MavenModelDependencyManagement(Model model) {
		this.dependencies = extractDependenciesFromModel(model);
		for (Dependency dependency : this.dependencies) {
			this.byArtifactId.put(dependency.getArtifactId(), dependency);
		}
	}

	private static List<Dependency> extractDependenciesFromModel(Model model) {
		List<Dependency> dependencies = new ArrayList<>();
		for (org.apache.maven.model.Dependency mavenDependency : model
				.getDependencyManagement().getDependencies()) {
			List<Exclusion> exclusions = new ArrayList<>();
			for (org.apache.maven.model.Exclusion mavenExclusion : mavenDependency
					.getExclusions()) {
				exclusions.add(new Exclusion(mavenExclusion.getGroupId(),
						mavenExclusion.getArtifactId()));
			}
			Dependency dependency = new Dependency(mavenDependency.getGroupId(),
					mavenDependency.getArtifactId(), mavenDependency.getVersion(),
					exclusions);
			dependencies.add(dependency);
		}
		return dependencies;
	}

	@Override
	public List<Dependency> getDependencies() {
		return this.dependencies;
	}

	@Override
	public String getSpringBootVersion() {
		return find("spring-boot").getVersion();
	}

	@Override
	public Dependency find(String artifactId) {
		return this.byArtifactId.get(artifactId);
	}

}
