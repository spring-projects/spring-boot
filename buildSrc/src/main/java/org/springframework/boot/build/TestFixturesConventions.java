/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.plugins.JavaTestFixturesPlugin;

/**
 * Conventions that are applied in the presence of the {@link JavaTestFixturesPlugin}.
 * When the plugin is applied:
 *
 * <ul>
 * <li>Publishing of the test fixtures is disabled.
 * </ul>
 *
 * @author Andy Wilkinson
 */
class TestFixturesConventions {

	void apply(Project project) {
		project.getPlugins().withType(JavaTestFixturesPlugin.class, (testFixtures) -> disablePublishing(project));
	}

	private void disablePublishing(Project project) {
		ConfigurationContainer configurations = project.getConfigurations();
		AdhocComponentWithVariants javaComponent = (AdhocComponentWithVariants) project.getComponents()
			.getByName("java");
		javaComponent.withVariantsFromConfiguration(configurations.getByName("testFixturesApiElements"),
				(variant) -> variant.skip());
		javaComponent.withVariantsFromConfiguration(configurations.getByName("testFixturesRuntimeElements"),
				(variant) -> variant.skip());
	}

}
