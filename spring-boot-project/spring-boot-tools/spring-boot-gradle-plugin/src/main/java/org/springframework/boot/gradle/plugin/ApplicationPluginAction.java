/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.concurrent.Callable;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.distribution.Distribution;
import org.gradle.api.distribution.DistributionContainer;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.IConventionAware;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.ApplicationPluginConvention;
import org.gradle.jvm.application.scripts.TemplateBasedScriptGenerator;

import org.springframework.boot.gradle.tasks.application.CreateBootStartScripts;

/**
 * Action that is executed in response to the {@link ApplicationPlugin} being applied.
 *
 * @author Andy Wilkinson
 */
final class ApplicationPluginAction implements PluginApplicationAction {

	@Override
	public void execute(Project project) {
		ApplicationPluginConvention applicationConvention = project.getConvention()
				.getPlugin(ApplicationPluginConvention.class);
		DistributionContainer distributions = project.getExtensions().getByType(DistributionContainer.class);
		Distribution distribution = distributions.create("boot");
		if (distribution instanceof IConventionAware) {
			((IConventionAware) distribution).getConventionMapping().map("baseName",
					() -> applicationConvention.getApplicationName() + "-boot");
		}
		CreateBootStartScripts bootStartScripts = project.getTasks().create("bootStartScripts",
				CreateBootStartScripts.class);
		bootStartScripts.setDescription(
				"Generates OS-specific start scripts to run the" + " project as a Spring Boot application.");
		((TemplateBasedScriptGenerator) bootStartScripts.getUnixStartScriptGenerator())
				.setTemplate(project.getResources().getText().fromString(loadResource("/unixStartScript.txt")));
		((TemplateBasedScriptGenerator) bootStartScripts.getWindowsStartScriptGenerator())
				.setTemplate(project.getResources().getText().fromString(loadResource("/windowsStartScript.txt")));
		project.getConfigurations().all((configuration) -> {
			if ("bootArchives".equals(configuration.getName())) {
				CopySpec libCopySpec = project.copySpec().into("lib")
						.from((Callable<FileCollection>) () -> configuration.getArtifacts().getFiles());
				libCopySpec.setFileMode(0644);
				distribution.getContents().with(libCopySpec);
				bootStartScripts.setClasspath(configuration.getArtifacts().getFiles());
			}
		});
		bootStartScripts.getConventionMapping().map("outputDir", () -> new File(project.getBuildDir(), "bootScripts"));
		bootStartScripts.getConventionMapping().map("applicationName", applicationConvention::getApplicationName);
		bootStartScripts.getConventionMapping().map("defaultJvmOpts",
				applicationConvention::getApplicationDefaultJvmArgs);
		CopySpec binCopySpec = project.copySpec().into("bin").from(bootStartScripts);
		binCopySpec.setFileMode(0755);
		distribution.getContents().with(binCopySpec);
	}

	@Override
	public Class<? extends Plugin<Project>> getPluginClass() {
		return ApplicationPlugin.class;
	}

	private String loadResource(String name) {
		try (InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream(name))) {
			char[] buffer = new char[4096];
			int read = 0;
			StringWriter writer = new StringWriter();
			while ((read = reader.read(buffer)) > 0) {
				writer.write(buffer, 0, read);
			}
			return writer.toString();
		}
		catch (IOException ex) {
			throw new GradleException("Failed to read '" + name + "'", ex);
		}
	}

}
