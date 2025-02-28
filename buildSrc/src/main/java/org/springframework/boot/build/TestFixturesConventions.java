/*
 * Copyright 2012-2025 the original author or authors.
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
