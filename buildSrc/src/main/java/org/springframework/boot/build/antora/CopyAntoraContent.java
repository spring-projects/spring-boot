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

package org.springframework.boot.build.antora;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/**
 * Tasks to copy Antora content.
 *
 * @author Andy Wilkinson
 */
public abstract class CopyAntoraContent extends DefaultTask {

	private FileCollection source;

	@Inject
	public CopyAntoraContent() {
	}

	@InputFiles
	public FileCollection getSource() {
		return this.source;
	}

	public void setSource(FileCollection source) {
		this.source = source;
	}

	@OutputFile
	public abstract RegularFileProperty getOutputFile();

	@TaskAction
	void copyAntoraContent() throws IllegalStateException, IOException {
		Path source = this.source.getSingleFile().toPath();
		Path target = getOutputFile().getAsFile().get().toPath();
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
	}

}
