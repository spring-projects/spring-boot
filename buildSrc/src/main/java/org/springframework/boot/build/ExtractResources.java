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

package org.springframework.boot.build;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import org.springframework.util.FileCopyUtils;
import org.springframework.util.PropertyPlaceholderHelper;

/**
 * {@link Task} to extract resources from the classpath and write them to disk.
 *
 * @author Andy Wilkinson
 */
public abstract class ExtractResources extends DefaultTask {

	private final PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");

	@Input
	public abstract ListProperty<String> getResourceNames();

	@OutputDirectory
	public abstract DirectoryProperty getDestinationDirectory();

	@Input
	public abstract MapProperty<String, String> getProperties();

	@TaskAction
	void extractResources() throws IOException {
		for (String resourceName : getResourceNames().get()) {
			InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourceName);
			if (resourceStream == null) {
				throw new GradleException("Resource '" + resourceName + "' does not exist");
			}
			String resource = FileCopyUtils.copyToString(new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
			resource = this.propertyPlaceholderHelper.replacePlaceholders(resource, getProperties().get()::get);
			FileCopyUtils.copy(resource,
					new FileWriter(getDestinationDirectory().file(resourceName).get().getAsFile()));
		}
	}

}
