/*
 * Copyright 2012-2022 the original author or authors.
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
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.distribution.Distribution;
import org.gradle.api.distribution.DistributionContainer;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.application.scripts.TemplateBasedScriptGenerator;
import org.gradle.jvm.application.tasks.CreateStartScripts;

/**
 * Action that is executed in response to the {@link ApplicationPlugin} being applied.
 *
 * @author Andy Wilkinson
 */
final class ApplicationPluginAction implements PluginApplicationAction {

	@Override
	public void execute(Project project) {
		JavaApplication javaApplication = project.getExtensions().getByType(JavaApplication.class);
		DistributionContainer distributions = project.getExtensions().getByType(DistributionContainer.class);
		Distribution distribution = distributions.create("boot");
		distribution.getDistributionBaseName()
				.convention((project.provider(() -> javaApplication.getApplicationName() + "-boot")));
		TaskProvider<CreateStartScripts> bootStartScripts = project.getTasks().register("bootStartScripts",
				CreateStartScripts.class,
				(task) -> configureCreateStartScripts(project, javaApplication, distribution, task));
		CopySpec binCopySpec = project.copySpec().into("bin").from(bootStartScripts);
		binCopySpec.setFileMode(0755);
		distribution.getContents().with(binCopySpec);
	}

	private void configureCreateStartScripts(Project project, JavaApplication javaApplication,
			Distribution distribution, CreateStartScripts createStartScripts) {
		createStartScripts
				.setDescription("Generates OS-specific start scripts to run the project as a Spring Boot application.");
		((TemplateBasedScriptGenerator) createStartScripts.getUnixStartScriptGenerator())
				.setTemplate(project.getResources().getText().fromString(loadResource("/unixStartScript.txt")));
		((TemplateBasedScriptGenerator) createStartScripts.getWindowsStartScriptGenerator())
				.setTemplate(project.getResources().getText().fromString(loadResource("/windowsStartScript.txt")));
		project.getConfigurations().all((configuration) -> {
			if ("bootArchives".equals(configuration.getName())) {
				distribution.getContents().with(artifactFilesToLibCopySpec(project, configuration));
				createStartScripts.setClasspath(configuration.getArtifacts().getFiles());
			}
		});
		createStartScripts.getConventionMapping().map("outputDir",
				() -> new File(project.getBuildDir(), "bootScripts"));
		createStartScripts.getConventionMapping().map("applicationName", javaApplication::getApplicationName);
		createStartScripts.getConventionMapping().map("defaultJvmOpts", javaApplication::getApplicationDefaultJvmArgs);
	}

	private CopySpec artifactFilesToLibCopySpec(Project project, Configuration configuration) {
		CopySpec copySpec = project.copySpec().into("lib").from(artifactFiles(configuration));
		copySpec.setFileMode(0644);
		return copySpec;
	}

	private Callable<FileCollection> artifactFiles(Configuration configuration) {
		return () -> configuration.getArtifacts().getFiles();
	}

	@Override
	public Class<? extends Plugin<Project>> getPluginClass() {
		return ApplicationPlugin.class;
	}

	private String loadResource(String name) {
		try (InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream(name))) {
			char[] buffer = new char[4096];
			int read;
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
