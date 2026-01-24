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

package org.springframework.boot.build;

import org.gradle.api.DomainObjectCollection;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.plugins.ide.api.XmlFileContentMerger;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.Classpath;
import org.gradle.plugins.ide.eclipse.model.ClasspathEntry;
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath;
import org.gradle.plugins.ide.eclipse.model.EclipseJdt;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.eclipse.model.Library;

/**
 * Conventions that are applied in the presence of the {@link EclipsePlugin} to work
 * around buildship issue {@code #1238}.
 *
 * @author Phillip Webb
 */
class EclipseConventions {

	void apply(Project project) {
		project.getPlugins().withType(EclipsePlugin.class, (eclipse) -> configure(project, eclipse));
	}

	private DomainObjectCollection<JavaBasePlugin> configure(Project project, EclipsePlugin eclipsePlugin) {
		TaskProvider<EclipseSynchronizeJdtSettings> eclipseSynchronizeJdtSettings = registerEclipseSynchronizeJdtSettingsTask(
				project);
		return project.getPlugins().withType(JavaBasePlugin.class, (javaBase) -> {
			EclipseModel model = project.getExtensions().getByType(EclipseModel.class);
			model.synchronizationTasks(eclipseSynchronizeJdtSettings);
			model.jdt(this::configureJdt);
			model.classpath(this::configureClasspath);
		});
	}

	private TaskProvider<EclipseSynchronizeJdtSettings> registerEclipseSynchronizeJdtSettingsTask(Project project) {
		TaskProvider<EclipseSynchronizeJdtSettings> taskProvider = project.getTasks()
			.register("eclipseSynchronizateJdt", EclipseSynchronizeJdtSettings.class);
		taskProvider.configure((task) -> {
			task.setDescription("Synchronizate the Eclipse JDT settings file from Buildship.");
			task.setOutputFile(project.file(".settings/org.eclipse.jdt.core.prefs"));
			task.setInputFile(project.file(".settings/org.eclipse.jdt.core.prefs"));
		});
		return taskProvider;
	}

	private void configureJdt(EclipseJdt jdt) {
		jdt.setSourceCompatibility(JavaVersion.toVersion(JavaConventions.RUNTIME_JAVA_VERSION));
		jdt.setTargetCompatibility(JavaVersion.toVersion(JavaConventions.RUNTIME_JAVA_VERSION));
		jdt.setJavaRuntimeName("JavaSE-" + JavaConventions.BUILD_JAVA_VERSION);
	}

	private void configureClasspath(EclipseClasspath classpath) {
		classpath.file(this::configureClasspathFile);
	}

	private void configureClasspathFile(XmlFileContentMerger merger) {
		merger.whenMerged((content) -> {
			if (content instanceof Classpath classpath) {
				classpath.getEntries().removeIf(this::isKotlinPluginContributedBuildDirectory);
			}
		});
	}

	private boolean isKotlinPluginContributedBuildDirectory(ClasspathEntry entry) {
		return (entry instanceof Library library) && isKotlinPluginContributedBuildDirectory(library.getPath())
				&& isTest(library);
	}

	private boolean isKotlinPluginContributedBuildDirectory(String path) {
		return path.contains("/main") && (path.contains("/build/classes/") || path.contains("/build/resources/"));
	}

	private boolean isTest(Library library) {
		Object value = library.getEntryAttributes().get("test");
		return (value instanceof String string && Boolean.parseBoolean(string));
	}

}
