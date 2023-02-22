/*
 * Copyright 2022-2023 the original author or authors.
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

package org.springframework.boot.build.architecture;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.library.dependencies.SliceRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

import org.springframework.util.FileCopyUtils;

/**
 * {@link Task} that checks for package tangles.
 *
 * @author Andy Wilkinson
 */
public abstract class PackageTangleCheck extends DefaultTask {

	private FileCollection classes;

	public PackageTangleCheck() {
		getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir(getName()));
	}

	@TaskAction
	void checkForPackageTangles() throws IOException {
		JavaClasses javaClasses = new ClassFileImporter()
			.importPaths(this.classes.getFiles().stream().map(File::toPath).collect(Collectors.toList()));
		SliceRule freeOfCycles = SlicesRuleDefinition.slices().matching("(**)").should().beFreeOfCycles();
		EvaluationResult result = freeOfCycles.evaluate(javaClasses);
		File outputFile = getOutputDirectory().file("failure-report.txt").get().getAsFile();
		outputFile.getParentFile().mkdirs();
		if (result.hasViolation()) {
			Files.writeString(outputFile.toPath(), result.getFailureReport().toString(), StandardOpenOption.CREATE);
			FileWriter writer = new FileWriter(outputFile);
			FileCopyUtils.copy(result.getFailureReport().toString(), writer);
			throw new GradleException("Package tangle check failed. See '" + outputFile + "' for details.");
		}
		else {
			outputFile.createNewFile();
		}
	}

	public void setClasses(FileCollection classes) {
		this.classes = classes;
	}

	@Internal
	public FileCollection getClasses() {
		return this.classes;
	}

	@InputFiles
	@SkipWhenEmpty
	@IgnoreEmptyDirectories
	@PathSensitive(PathSensitivity.RELATIVE)
	final FileTree getInputClasses() {
		return this.classes.getAsFileTree();
	}

	@OutputDirectory
	public abstract DirectoryProperty getOutputDirectory();

}
