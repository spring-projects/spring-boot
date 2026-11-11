/*
 * Copyright 2012-present the original author or authors.
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
