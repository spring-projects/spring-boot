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

package org.springframework.boot.gradle.plugin;

import org.cyclonedx.gradle.CyclonedxAggregateTask;
import org.cyclonedx.gradle.CyclonedxPlugin;
import org.cyclonedx.model.Component;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

import org.springframework.boot.gradle.tasks.bundling.BootJar;
import org.springframework.boot.gradle.tasks.bundling.BootWar;

/**
 * {@link Action} that is executed in response to the {@link CyclonedxPlugin} being
 * applied.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
final class CyclonedxPluginAction implements PluginApplicationAction {

	@Override
	public Class<? extends Plugin<? extends Project>> getPluginClass() {
		return CyclonedxPlugin.class;
	}

	@Override
	public void execute(Project project) {
		TaskProvider<CyclonedxAggregateTask> cycloneDxTaskProvider = project.getTasks()
			.named("cyclonedxBom", CyclonedxAggregateTask.class);
		configureCycloneDxTask(cycloneDxTaskProvider, project);
		configureJavaPlugin(project, cycloneDxTaskProvider);
		configureSpringBootPlugin(project, cycloneDxTaskProvider);
	}

	private void configureCycloneDxTask(TaskProvider<CyclonedxAggregateTask> taskProvider, Project project) {
		taskProvider.configure((task) -> {
			task.getProjectType().convention(Component.Type.APPLICATION);
			task.getXmlOutput().unsetConvention();
			task.getJsonOutput()
				.convention(project.getLayout().getBuildDirectory().file("reports/cyclonedx/application.cdx.json"));
			task.getIncludeLicenseText().convention(false);
		});
	}

	private void configureJavaPlugin(Project project, TaskProvider<CyclonedxAggregateTask> cycloneDxTaskProvider) {
		configurePlugin(project, JavaPlugin.class, (javaPlugin) -> {
			JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
			SourceSet main = javaPluginExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
			configureTask(project, main.getProcessResourcesTaskName(), Copy.class, (copy) -> {
				copy.dependsOn(cycloneDxTaskProvider);
				Provider<String> sbomFileName = cycloneDxTaskProvider.flatMap(
						(cycloneDxTask) -> cycloneDxTask.getJsonOutput().map((file) -> file.getAsFile().getName()));
				copy.from(cycloneDxTaskProvider,
						(spec) -> spec.include((element) -> element.getName().equals(sbomFileName.get()))
							.into("META-INF/sbom"));
			});
		});
	}

	private void configureSpringBootPlugin(Project project,
			TaskProvider<CyclonedxAggregateTask> cycloneDxTaskProvider) {
		configurePlugin(project, SpringBootPlugin.class, (springBootPlugin) -> {
			configureBootJarTask(project, cycloneDxTaskProvider);
			configureBootWarTask(project, cycloneDxTaskProvider);
		});
	}

	private void configureBootJarTask(Project project, TaskProvider<CyclonedxAggregateTask> cycloneDxTaskProvider) {
		configureTask(project, SpringBootPlugin.BOOT_JAR_TASK_NAME, BootJar.class,
				(bootJar) -> configureBootJarTask(bootJar, cycloneDxTaskProvider));
	}

	private void configureBootWarTask(Project project, TaskProvider<CyclonedxAggregateTask> cycloneDxTaskProvider) {
		configureTask(project, SpringBootPlugin.BOOT_WAR_TASK_NAME, BootWar.class,
				(bootWar) -> configureBootWarTask(bootWar, cycloneDxTaskProvider));
	}

	private void configureBootJarTask(BootJar task, TaskProvider<CyclonedxAggregateTask> cycloneDxTaskProvider) {
		configureJarTask(task, cycloneDxTaskProvider, "");
	}

	private void configureBootWarTask(BootWar task, TaskProvider<CyclonedxAggregateTask> cycloneDxTaskProvider) {
		configureJarTask(task, cycloneDxTaskProvider, "WEB-INF/classes/");
	}

	private void configureJarTask(Jar task, TaskProvider<CyclonedxAggregateTask> cycloneDxTaskProvider,
			String sbomLocationPrefix) {
		Provider<String> sbomFileName = cycloneDxTaskProvider
			.map((cycloneDxTask) -> "META-INF/sbom/" + cycloneDxTask.getJsonOutput().get().getAsFile().getName());
		task.manifest((manifest) -> {
			manifest.getAttributes().put("Sbom-Format", "CycloneDX");
			manifest.getAttributes()
				.put("Sbom-Location", sbomFileName.map((fileName) -> sbomLocationPrefix + fileName));
		});
	}

	private <T extends Task> void configureTask(Project project, String name, Class<T> type, Action<T> action) {
		project.getTasks().withType(type).configureEach((task) -> {
			if (task.getName().equals(name)) {
				action.execute(task);
			}
		});
	}

	private <T extends Plugin<?>> void configurePlugin(Project project, Class<T> plugin, Action<T> action) {
		project.getPlugins().withType(plugin, action);
	}

}
