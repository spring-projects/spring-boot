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

package org.springframework.boot.gradle.plugin;

import org.cyclonedx.gradle.CycloneDxPlugin;
import org.cyclonedx.gradle.CycloneDxTask;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import org.springframework.boot.gradle.tasks.bundling.BootJar;

/**
 * {@link Action} that is executed in response to the {@link CycloneDxPlugin} being
 * applied.
 *
 * @author Moritz Halbritter
 */
final class CycloneDxPluginAction implements PluginApplicationAction {

	@Override
	public Class<? extends Plugin<? extends Project>> getPluginClass() {
		return CycloneDxPlugin.class;
	}

	@Override
	public void execute(Project project) {
		TaskProvider<CycloneDxTask> cyclonedxBom = project.getTasks().named("cyclonedxBom", CycloneDxTask.class);
		cyclonedxBom.configure((task) -> {
			task.getProjectType().convention("application");
			task.getOutputFormat().convention("json");
			task.getOutputName().convention("application.cdx");
			task.getIncludeLicenseText().convention(false);
		});
		project.getTasks().named(SpringBootPlugin.BOOT_JAR_TASK_NAME, BootJar.class).configure((bootJar) -> {
			CycloneDxTask cycloneDxTask = cyclonedxBom.get();
			String sbomFileName = cycloneDxTask.getOutputName().get() + getSbomExtension(cycloneDxTask);
			bootJar.from(cycloneDxTask, (spec) -> spec.include(sbomFileName).into("META-INF/sbom"));
			bootJar.manifest((manifest) -> {
				manifest.getAttributes().put("Sbom-Format", "CycloneDX");
				manifest.getAttributes().put("Sbom-Location", "META-INF/sbom/" + sbomFileName);
			});
		});
	}

	private String getSbomExtension(CycloneDxTask task) {
		String format = task.getOutputFormat().get();
		if ("all".equals(format)) {
			return ".json";
		}
		return "." + format;
	}

}
