/*
 * Copyright 2012-2021 the original author or authors.
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

import org.springframework.boot.build.constraints.ExtractVersionConstraints.ConstrainedVersion;

/**
 * Task for documenting a platform's constrained versions.
 *
 * @author Andy Wilkinson
 */
public class DocumentConstrainedVersions extends DefaultTask {

	private final SetProperty<ConstrainedVersion> constrainedVersions;

	private File outputFile;

	/**
     * Constructs a new instance of DocumentConstrainedVersions with the specified objectFactory.
     * 
     * @param objectFactory the object factory used to create instances of ConstrainedVersion
     */
    @Inject
	public DocumentConstrainedVersions(ObjectFactory objectFactory) {
		this.constrainedVersions = objectFactory.setProperty(ConstrainedVersion.class);
	}

	/**
     * Retrieves the set of constrained versions.
     *
     * @return The set of constrained versions.
     */
    @Input
	public SetProperty<ConstrainedVersion> getConstrainedVersions() {
		return this.constrainedVersions;
	}

	/**
     * Returns the output file.
     *
     * @return the output file
     */
    @OutputFile
	public File getOutputFile() {
		return this.outputFile;
	}

	/**
     * Sets the output file for the document.
     * 
     * @param outputFile the file to set as the output file
     */
    public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	/**
     * Generates a Javadoc style documentation for the method documentConstrainedVersions.
     * This method is responsible for documenting the constrained versions of artifacts.
     *
     * @throws IOException if an I/O error occurs while writing the documentation to the output file.
     */
    @TaskAction
	public void documentConstrainedVersions() throws IOException {
		this.outputFile.getParentFile().mkdirs();
		try (PrintWriter writer = new PrintWriter(new FileWriter(this.outputFile))) {
			writer.println("|===");
			writer.println("| Group ID | Artifact ID | Version");
			for (ConstrainedVersion constrainedVersion : this.constrainedVersions.get()) {
				writer.println();
				writer.printf("| `%s`%n", constrainedVersion.getGroup());
				writer.printf("| `%s`%n", constrainedVersion.getArtifact());
				writer.printf("| `%s`%n", constrainedVersion.getVersion());
			}
			writer.println("|===");
		}
	}

}
