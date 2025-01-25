/*
 * Copyright 2012-2025 the original author or authors.
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.Transformer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

/**
 * {@link Task} that checks for architecture problems.
 *
 * @author Andy Wilkinson
 * @author Yanming Zhou
 * @author Scott Frederick
 * @author Ivan Malutin
 * @author Phillip Webb
 */
public abstract class ArchitectureCheck extends DefaultTask {

	private FileCollection classes;

	public ArchitectureCheck() {
		getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir(getName()));
		getRules().addAll(getProhibitObjectsRequireNonNull().convention(true)
			.map(whenTrue(ArchitectureRules::noClassesShouldCallObjectsRequireNonNull)));
		getRules().addAll(ArchitectureRules.standard());
		getRuleDescriptions().set(getRules().map(this::asDescriptions));
	}

	private Transformer<List<ArchRule>, Boolean> whenTrue(Supplier<List<ArchRule>> rules) {
		return (in) -> (!in) ? Collections.emptyList() : rules.get();
	}

	private List<String> asDescriptions(List<ArchRule> rules) {
		return rules.stream().map(ArchRule::getDescription).toList();
	}

	@TaskAction
	void checkArchitecture() throws IOException {
		JavaClasses javaClasses = new ClassFileImporter().importPaths(classFilesPaths());
		List<EvaluationResult> violations = evaluate(javaClasses).filter(EvaluationResult::hasViolation).toList();
		File outputFile = getOutputDirectory().file("failure-report.txt").get().getAsFile();
		writeViolationReport(violations, outputFile);
		if (!violations.isEmpty()) {
			throw new GradleException("Architecture check failed. See '" + outputFile + "' for details.");
		}
	}

	private List<Path> classFilesPaths() {
		return this.classes.getFiles().stream().map(File::toPath).toList();
	}

	private Stream<EvaluationResult> evaluate(JavaClasses javaClasses) {
		return getRules().get().stream().map((rule) -> rule.evaluate(javaClasses));
	}

	private void writeViolationReport(List<EvaluationResult> violations, File outputFile) throws IOException {
		outputFile.getParentFile().mkdirs();
		StringBuilder report = new StringBuilder();
		for (EvaluationResult violation : violations) {
			report.append(violation.getFailureReport());
			report.append(String.format("%n"));
		}
		Files.writeString(outputFile.toPath(), report.toString(), StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);
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

	@Optional
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract DirectoryProperty getResourcesDirectory();

	@OutputDirectory
	public abstract DirectoryProperty getOutputDirectory();

	@Internal
	public abstract ListProperty<ArchRule> getRules();

	@Internal
	public abstract Property<Boolean> getProhibitObjectsRequireNonNull();

	@Input // Use descriptions as input since rules aren't serializable
	abstract ListProperty<String> getRuleDescriptions();

}
