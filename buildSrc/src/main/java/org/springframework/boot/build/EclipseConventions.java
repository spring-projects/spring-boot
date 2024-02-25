/*
 * Copyright 2023-2023 the original author or authors.
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
import org.gradle.plugins.ide.api.XmlFileContentMerger;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.Classpath;
import org.gradle.plugins.ide.eclipse.model.ClasspathEntry;
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.eclipse.model.Library;

/**
 * Conventions that are applied in the presence of the {@link EclipsePlugin} to work
 * around buildship issue {@code #1238}.
 *
 * @author Phillip Webb
 */
class EclipseConventions {

	/**
     * Applies the Eclipse plugin to the given project and configures the classpath for the Eclipse model.
     * 
     * @param project the project to apply the Eclipse plugin to
     */
    void apply(Project project) {
		project.getPlugins().withType(EclipsePlugin.class, (eclipse) -> {
			EclipseModel eclipseModel = project.getExtensions().getByType(EclipseModel.class);
			eclipseModel.classpath(this::configureClasspath);
		});
	}

	/**
     * Configures the classpath for the Eclipse project.
     * 
     * @param classpath The EclipseClasspath object representing the classpath to be configured.
     */
    private void configureClasspath(EclipseClasspath classpath) {
		classpath.file(this::configureClasspathFile);
	}

	/**
     * Configures the classpath file by removing Kotlin plugin contributed build directories.
     * 
     * @param merger the XmlFileContentMerger used to merge the content of the classpath file
     */
    private void configureClasspathFile(XmlFileContentMerger merger) {
		merger.whenMerged((content) -> {
			if (content instanceof Classpath classpath) {
				classpath.getEntries().removeIf(this::isKotlinPluginContributedBuildDirectory);
			}
		});
	}

	/**
     * Checks if the given classpath entry is a Kotlin plugin contributed build directory.
     * 
     * @param entry the classpath entry to check
     * @return true if the entry is a Kotlin plugin contributed build directory, false otherwise
     */
    private boolean isKotlinPluginContributedBuildDirectory(ClasspathEntry entry) {
		return (entry instanceof Library library) && isKotlinPluginContributedBuildDirectory(library.getPath())
				&& isTest(library);
	}

	/**
     * Checks if the given path is a Kotlin plugin contributed build directory.
     * 
     * @param path the path to be checked
     * @return true if the path is a Kotlin plugin contributed build directory, false otherwise
     */
    private boolean isKotlinPluginContributedBuildDirectory(String path) {
		return path.contains("/main") && (path.contains("/build/classes/") || path.contains("/build/resources/"));
	}

	/**
     * Checks if the given library is a test library.
     * 
     * @param library the library to check
     * @return true if the library is a test library, false otherwise
     */
    private boolean isTest(Library library) {
		Object value = library.getEntryAttributes().get("test");
		return (value instanceof String string && Boolean.parseBoolean(string));
	}

}
