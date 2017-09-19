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

package org.springframework.boot.gradle.tasks.application;

import java.io.File;

import org.gradle.api.provider.PropertyState;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.jvm.application.tasks.CreateStartScripts;

/**
 * Customization of {@link CreateStartScripts} that makes the {@link #getMainClassName()
 * main class name} optional.
 *
 * @author Andy Wilkinson
 */
public class CreateBootStartScripts extends CreateStartScripts {

	private final PropertyState<File> outputDir = getProject().property(File.class);

	private final PropertyState<String> applicationName = getProject()
			.property(String.class);

	@Override
	@Optional
	public String getMainClassName() {
		return super.getMainClassName();
	}

	@Input
	@Override
	public String getApplicationName() {
		return this.applicationName.getOrNull();
	}

	@Override
	public void setApplicationName(String applicationName) {
		this.applicationName.set(applicationName);
	}

	/**
	 * Sets the application name to the value from the given
	 * {@code applicationNameProvider}.
	 * @param applicationNameProvider the provider of the application name
	 */
	public void setApplicationName(Provider<String> applicationNameProvider) {
		this.applicationName.set(applicationNameProvider);
	}

	@Override
	@OutputDirectory
	public File getOutputDir() {
		return this.outputDir.getOrNull();
	}

	@Override
	public void setOutputDir(File outputDir) {
		this.outputDir.set(outputDir);
	}

	/**
	 * Sets the output directory to the value from the given {@code outputDirProvider}.
	 * @param outputDirProvider the provider of the output directory
	 */
	public void setOutputDir(Provider<File> outputDirProvider) {
		this.outputDir.set(outputDirProvider);
	}

}
