/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.aggregation;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ResolvableConfiguration;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.model.ObjectFactory;

/**
 * {@link Plugin} for aggregating the output of other projects.
 *
 * @author Andy Wilkinson
 */
public class AggregatorPlugin implements Plugin<Project> {

	@Override
	public void apply(Project target) {
		NamedDomainObjectContainer<Aggregate> aggregates = target.getObjects().domainObjectContainer(Aggregate.class);
		target.getExtensions().add("aggregates", aggregates);
		aggregates.all((aggregate) -> {
			NamedDomainObjectProvider<DependencyScopeConfiguration> dependencies = target.getConfigurations()
				.dependencyScope(aggregate.getName() + "Dependencies");
			NamedDomainObjectProvider<ResolvableConfiguration> aggregated = target.getConfigurations()
				.resolvable(aggregate.getName(), (configuration) -> {
					configuration.extendsFrom(dependencies.get());
					configureAttributes(configuration, aggregate, target.getObjects());
				});
			target.getRootProject()
				.allprojects((project) -> target.getDependencies()
					.add(dependencies.getName(), project.getDependencyFactory().createProjectDependency()));
			aggregate.getFiles()
				.convention(aggregated.map((configuration) -> configuration.getIncoming()
					.artifactView((view) -> view.setLenient(true))
					.getFiles()));
		});
	}

	private void configureAttributes(Configuration configuration, Aggregate aggregate, ObjectFactory objects) {
		configuration.attributes((attributes) -> {
			attributes.attributeProvider(Category.CATEGORY_ATTRIBUTE,
					aggregate.getCategory().map((category) -> objects.named(Category.class, category)));
			attributes.attributeProvider(Usage.USAGE_ATTRIBUTE,
					aggregate.getUsage().map((usage) -> objects.named(Usage.class, usage)));
		});

	}

}
