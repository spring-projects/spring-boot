/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.boot.build.constraints;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import org.springframework.boot.build.constraints.ExtractVersionConstraints.VersionProperty;

/**
 * Task for documenting available version properties.
 *
 * @author Christoph Dreis
 */
public class DocumentVersionProperties extends DefaultTask {

	private final SetProperty<VersionProperty> versionProperties;

	private File outputFile;

	@Inject
	public DocumentVersionProperties(ObjectFactory objectFactory) {
		this.versionProperties = objectFactory.setProperty(VersionProperty.class);
	}

	@Input
	public SetProperty<VersionProperty> getVersionProperties() {
		return this.versionProperties;
	}

	@OutputFile
	public File getOutputFile() {
		return this.outputFile;
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	@TaskAction
	public void documentVersionProperties() throws IOException {
		this.outputFile.getParentFile().mkdirs();
		try (PrintWriter writer = new PrintWriter(new FileWriter(this.outputFile))) {
			writer.println("|===");
			writer.println("| Library | Version Property");
			for (VersionProperty versionProperty : this.versionProperties.get()) {
				writer.println();
				writer.printf("| `%s`%n", versionProperty.getLibraryName());
				writer.printf("| `%s`%n", versionProperty.getVersionProperty());
			}
			writer.println("|===");
		}
	}

}
