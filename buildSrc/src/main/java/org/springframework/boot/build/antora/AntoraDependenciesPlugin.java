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

package org.springframework.boot.build.antora;

import javax.inject.Inject;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * {@link Plugin} for a project that depends on {@link AntoraContributorPlugin
 * contributed} Antora-based documentation.
 *
 * @author Andy Wilkinson
 */
public class AntoraDependenciesPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		NamedDomainObjectContainer<AntoraDependency> antoraDependencies = project.getObjects()
			.domainObjectContainer(AntoraDependency.class);
		project.getExtensions().add("antoraDependencies", antoraDependencies);
	}

	public static class AntoraDependency {

		private final String name;

		private final Project project;

		private String path;

		@Inject
		public AntoraDependency(String name, Project project) {
			this.name = name;
			this.project = project;
		}

		public String getName() {
			return this.name;
		}

		public String getPath() {
			return this.path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public void catalogContent() {
			new CatalogContentContribution(this.project, this.name).consumeFrom(this.path);
		}

		public void aggregateContent() {
			new AggregateContentContribution(this.project, this.name).consumeFrom(this.path);
		}

		public void source() {
			new SourceContribution(this.project, this.name).consumeFrom(this.path);
		}

	}

}
