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

package org.springframework.boot.build.test.autoconfigure;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

/**
 * {@link Plugin} for projects that define one or more test slices. When applied, it:
 *
 * <ul>
 * <li>Applies the {@link TestAutoConfigurationPlugin}
 * </ul>
 * Additionally, when the {@link JavaPlugin} is applied it:
 *
 * <ul>
 * <li>Defines a task that produces metadata describing the test slices. The metadata is
 * made available as an artifact in the {@code testSliceMetadata} configuration
 * </ul>
 *
 * @author Andy Wilkinson
 */
public class TestSlicePlugin implements Plugin<Project> {

	private static final String TEST_SLICE_METADATA_CONFIGURATION_NAME = "testSliceMetadata";

	@Override
	public void apply(Project target) {
		PluginContainer plugins = target.getPlugins();
		plugins.apply(TestAutoConfigurationPlugin.class);
		plugins.withType(JavaPlugin.class, (plugin) -> {
			TaskProvider<GenerateTestSliceMetadata> generateTestSliceMetadata = target.getTasks()
				.register("generateTestSliceMetadata", GenerateTestSliceMetadata.class, (task) -> {
					SourceSet mainSourceSet = target.getExtensions()
						.getByType(JavaPluginExtension.class)
						.getSourceSets()
						.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
					task.setSourceSet(mainSourceSet);
					task.getOutputFile().set(target.getLayout().getBuildDirectory().file("test-slice-metadata.json"));
				});
			addMetadataArtifact(target, generateTestSliceMetadata);
		});
	}

	private void addMetadataArtifact(Project project, TaskProvider<GenerateTestSliceMetadata> task) {
		project.getConfigurations().consumable(TEST_SLICE_METADATA_CONFIGURATION_NAME, (configuration) -> {
			configuration.attributes((attributes) -> {
				attributes.attribute(Category.CATEGORY_ATTRIBUTE,
						project.getObjects().named(Category.class, Category.DOCUMENTATION));
				attributes.attribute(Usage.USAGE_ATTRIBUTE,
						project.getObjects().named(Usage.class, "test-slice-metadata"));
			});
		});
		project.getArtifacts().add(TEST_SLICE_METADATA_CONFIGURATION_NAME, task);
	}

}
