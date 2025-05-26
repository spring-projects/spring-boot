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

import org.antora.gradle.AntoraPlugin;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;

/**
 * {@link Plugin} for a project that contributes to Antora-based documentation that is
 * {@link AntoraDependenciesPlugin depended upon} by another project.
 *
 * @author Andy Wilkinson
 */
public class AntoraContributorPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		project.getPlugins().apply(AntoraPlugin.class);
		NamedDomainObjectContainer<Contribution> antoraContributions = project.getObjects()
			.domainObjectContainer(Contribution.class,
					(name) -> project.getObjects().newInstance(Contribution.class, name, project));
		project.getExtensions().add("antoraContributions", antoraContributions);
	}

	public static class Contribution {

		private final String name;

		private final Project project;

		private boolean publish;

		@Inject
		public Contribution(String name, Project project) {
			this.name = name;
			this.project = project;
		}

		public String getName() {
			return this.name;
		}

		public void publish() {
			this.publish = true;
		}

		public void source() {
			new SourceContribution(this.project, this.name).produce();
		}

		public void catalogContent(Action<CopySpec> action) {
			CopySpec copySpec = this.project.copySpec();
			action.execute(copySpec);
			new CatalogContentContribution(this.project, this.name).produceFrom(copySpec, this.publish);
		}

		public void aggregateContent(Action<CopySpec> action) {
			CopySpec copySpec = this.project.copySpec();
			action.execute(copySpec);
			new AggregateContentContribution(this.project, this.name).produceFrom(copySpec, this.publish);
		}

		public void localAggregateContent(Action<CopySpec> action) {
			CopySpec copySpec = this.project.copySpec();
			action.execute(copySpec);
			new LocalAggregateContentContribution(this.project, this.name).produceFrom(copySpec);
		}

	}

}
