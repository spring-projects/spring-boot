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
				.allprojects((project) -> target.getDependencies().add(dependencies.getName(), project));
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
