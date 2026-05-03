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

package org.springframework.boot.gradle.tasks.buildinfo;

import java.io.File;
import java.io.IOException;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.work.DisableCachingByDefault;

import org.springframework.boot.loader.tools.BuildPropertiesWriter;
import org.springframework.boot.loader.tools.BuildPropertiesWriter.ProjectDetails;

/**
 * {@link Task} for generating a build info properties file from
 * {@link BuildInfoProperties}.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@DisableCachingByDefault(because = "Not worth caching")
public abstract class BuildInfo extends DefaultTask {

	private final BuildInfoProperties properties;

	public BuildInfo() {
		this.properties = getProject().getObjects().newInstance(BuildInfoProperties.class, getExcludes());
		getDestinationDir().convention(getProject().getLayout().getBuildDirectory().dir(getName()));
		getFilename().convention("META-INF/build-info.properties");
	}

	/**
	 * Returns the names of the properties to exclude from the output.
	 * @return names of the properties to exclude
	 * @since 3.0.0
	 */
	@Internal
	public abstract SetProperty<String> getExcludes();

	/**
	 * Returns the name of the file that is written to the {@link #getDestinationDir
	 * destination dir}. Convention is {@code META-INF/build-info.properties}.
	 * @return the name of the written file
	 * @since 4.1.0
	 */
	@Input
	public abstract Property<String> getFilename();

	/**
	 * Generates the build info properties file in the configured
	 * {@link #getDestinationDir destination dir}.
	 */
	@TaskAction
	public void generateBuildProperties() {
		try {
			ProjectDetails details = new ProjectDetails(this.properties.getGroupIfNotExcluded(),
					this.properties.getArtifactIfNotExcluded(), this.properties.getVersionIfNotExcluded(),
					this.properties.getNameIfNotExcluded(), this.properties.getTimeIfNotExcluded(),
					this.properties.getAdditionalIfNotExcluded());
			File outputFile = new File(getDestinationDir().getAsFile().get(), getFilename().get());
			new BuildPropertiesWriter(outputFile).writeBuildProperties(details);
		}
		catch (IOException ex) {
			throw new TaskExecutionException(this, ex);
		}
	}

	/**
	 * Returns the directory to which the build info file will be written. Convention is
	 * <code>build/${taskName}</code>.
	 * @return the destination directory
	 */
	@OutputDirectory
	public abstract DirectoryProperty getDestinationDir();

	/**
	 * Returns the {@link BuildInfoProperties properties} that will be included in the
	 * written file.
	 * @return the properties
	 */
	@Nested
	public BuildInfoProperties getProperties() {
		return this.properties;
	}

	/**
	 * Executes the given {@code action} on the {@link #getProperties()} properties.
	 * @param action the action
	 */
	public void properties(Action<BuildInfoProperties> action) {
		action.execute(this.properties);
	}

}
