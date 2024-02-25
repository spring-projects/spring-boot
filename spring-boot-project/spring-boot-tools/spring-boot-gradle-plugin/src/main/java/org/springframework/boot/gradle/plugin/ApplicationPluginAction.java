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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.gradle.api.Action;
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
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.application.scripts.TemplateBasedScriptGenerator;
import org.gradle.jvm.application.tasks.CreateStartScripts;
import org.gradle.util.GradleVersion;

import org.springframework.boot.gradle.tasks.run.BootRun;

/**
 * Action that is executed in response to the {@link ApplicationPlugin} being applied.
 *
 * @author Andy Wilkinson
 */
final class ApplicationPluginAction implements PluginApplicationAction {

	/**
	 * Executes the given project by creating a boot distribution and configuring start
	 * scripts.
	 * @param project The project to execute.
	 */
	@Override
	public void execute(Project project) {
		JavaApplication javaApplication = project.getExtensions().getByType(JavaApplication.class);
		DistributionContainer distributions = project.getExtensions().getByType(DistributionContainer.class);
		Distribution distribution = distributions.create("boot");
		distribution.getDistributionBaseName()
			.convention((project.provider(() -> javaApplication.getApplicationName() + "-boot")));
		TaskProvider<CreateStartScripts> bootStartScripts = project.getTasks()
			.register("bootStartScripts", CreateStartScripts.class,
					(task) -> configureCreateStartScripts(project, javaApplication, distribution, task));
		CopySpec binCopySpec = project.copySpec().into("bin").from(bootStartScripts);
		configureFilePermissions(binCopySpec, 0755);
		distribution.getContents().with(binCopySpec);
		applyApplicationDefaultJvmArgsToRunTasks(project.getTasks(), javaApplication);
	}

	/**
	 * Applies the default JVM arguments to the run tasks of the given task container and
	 * Java application.
	 * @param tasks The task container containing the run tasks.
	 * @param javaApplication The Java application to apply the default JVM arguments to.
	 */
	private void applyApplicationDefaultJvmArgsToRunTasks(TaskContainer tasks, JavaApplication javaApplication) {
		applyApplicationDefaultJvmArgsToRunTask(tasks, javaApplication, SpringBootPlugin.BOOT_RUN_TASK_NAME);
		applyApplicationDefaultJvmArgsToRunTask(tasks, javaApplication, SpringBootPlugin.BOOT_TEST_RUN_TASK_NAME);
	}

	/**
	 * Applies the application default JVM arguments to the run task.
	 * @param tasks The task container.
	 * @param javaApplication The Java application.
	 * @param taskName The name of the task.
	 */
	private void applyApplicationDefaultJvmArgsToRunTask(TaskContainer tasks, JavaApplication javaApplication,
			String taskName) {
		tasks.named(taskName, BootRun.class)
			.configure((bootRun) -> bootRun.getConventionMapping()
				.map("jvmArgs", javaApplication::getApplicationDefaultJvmArgs));
	}

	/**
	 * Configures the creation of OS-specific start scripts for running the project as a
	 * Spring Boot application.
	 * @param project The project to configure.
	 * @param javaApplication The Java application to run.
	 * @param distribution The distribution to generate start scripts for.
	 * @param createStartScripts The object responsible for creating start scripts.
	 */
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
		createStartScripts.getConventionMapping()
			.map("outputDir", () -> project.getLayout().getBuildDirectory().dir("bootScripts").get().getAsFile());
		createStartScripts.getConventionMapping().map("applicationName", javaApplication::getApplicationName);
		createStartScripts.getConventionMapping().map("defaultJvmOpts", javaApplication::getApplicationDefaultJvmArgs);
	}

	/**
	 * Creates a CopySpec object to copy artifact files to the "lib" directory.
	 * @param project the project object
	 * @param configuration the configuration object
	 * @return the CopySpec object with configured file permissions
	 */
	private CopySpec artifactFilesToLibCopySpec(Project project, Configuration configuration) {
		CopySpec copySpec = project.copySpec().into("lib").from(artifactFiles(configuration));
		configureFilePermissions(copySpec, 0644);
		return copySpec;
	}

	/**
	 * Returns a Callable object that retrieves the artifact files from the given
	 * configuration.
	 * @param configuration the configuration from which to retrieve the artifact files
	 * @return a Callable<FileCollection> object that retrieves the artifact files
	 */
	private Callable<FileCollection> artifactFiles(Configuration configuration) {
		return () -> configuration.getArtifacts().getFiles();
	}

	/**
	 * Returns the class of the plugin that this action is associated with.
	 * @return the class of the plugin
	 */
	@Override
	public Class<? extends Plugin<Project>> getPluginClass() {
		return ApplicationPlugin.class;
	}

	/**
	 * Loads a resource with the given name.
	 * @param name the name of the resource to load
	 * @return the content of the resource as a string
	 * @throws GradleException if the resource cannot be read
	 */
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

	/**
	 * Configures the file permissions for the given CopySpec.
	 * @param copySpec The CopySpec to configure the file permissions for.
	 * @param mode The file mode to set.
	 * @throws GradleException If an error occurs while setting the file permissions.
	 */
	private void configureFilePermissions(CopySpec copySpec, int mode) {
		if (GradleVersion.current().compareTo(GradleVersion.version("8.3")) >= 0) {
			try {
				Method filePermissions = copySpec.getClass().getMethod("filePermissions", Action.class);
				filePermissions.invoke(copySpec, new Action<>() {

					@Override
					public void execute(Object filePermissions) {
						String unixPermissions = Integer.toString(mode, 8);
						try {
							Method unix = filePermissions.getClass().getMethod("unix", String.class);
							unix.invoke(filePermissions, unixPermissions);
						}
						catch (Exception ex) {
							throw new GradleException("Failed to set file permissions to '" + unixPermissions + "'",
									ex);
						}
					}

				});
			}
			catch (Exception ex) {
				throw new GradleException("Failed to set file permissions", ex);
			}
		}
		else {
			copySpec.setFileMode(mode);
		}
	}

}
