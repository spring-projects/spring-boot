/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.tasks.bundling;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

/**
 * A Spring Boot "fat" archive task.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public interface BootArchive extends Task {

	/**
	 * Returns the name of the main class of the application.
	 *
	 * @return the main class name
	 */
	@Input
	@Optional
	String getMainClassName();

	/**
	 * Sets the name of the main class of the application.
	 *
	 * @param mainClassName the name of the main class of the application
	 */
	void setMainClassName(String mainClassName);

	/**
	 * Adds Ant-style patterns that identify files that must be unpacked from the archive
	 * when it is launched.
	 *
	 * @param patterns the patterns
	 */
	void requiresUnpack(String... patterns);

	/**
	 * Adds a spec that identifies files that must be unpacked from the archive when it is
	 * launched.
	 *
	 * @param spec the spec
	 */
	void requiresUnpack(Spec<FileTreeElement> spec);

	/**
	 * Returns the {@link LaunchScriptConfiguration} that will control the script that is
	 * prepended to the archive.
	 *
	 * @return the launch script configuration, or {@code null} if the launch script has
	 * not been configured.
	 */
	@Input
	@Optional
	LaunchScriptConfiguration getLaunchScript();

	/**
	 * Configures the archive to have a prepended launch script.
	 */
	void launchScript();

	/**
	 * Configures the archive to have a prepended launch script, customizing its
	 * configuration using the given {@code action}.
	 *
	 * @param action the action to apply
	 */
	void launchScript(Action<LaunchScriptConfiguration> action);

	/**
	 * Returns the classpath that will be included in the archive.
	 *
	 * @return the classpath
	 */
	@Optional
	@Classpath
	FileCollection getClasspath();

	/**
	 * Adds files to the classpath to include in the archive. The given {@code classpath}
	 * are evaluated as per {@link Project#files(Object...)}.
	 *
	 * @param classpath the additions to the classpath
	 */
	void classpath(Object... classpath);

	/**
	 * Returns {@code true} if the Devtools jar should be excluded, otherwise
	 * {@code false}.
	 *
	 * @return {@code true} if the Devtools jar should be excluded, or {@code false} if
	 * not
	 */
	@Input
	boolean isExcludeDevtools();

	/**
	 * Sets whether or not the Devtools jar should be excluded.
	 *
	 * @param excludeDevtools {@code true} if the Devtools jar should be excluded, or
	 * {@code false} if not
	 */
	void setExcludeDevtools(boolean excludeDevtools);

}
