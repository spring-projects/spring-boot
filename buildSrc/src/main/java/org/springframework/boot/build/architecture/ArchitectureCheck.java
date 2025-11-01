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

package org.springframework.boot.build.architecture;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.Transformer;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;

/**
 * {@link Task} that checks for architecture problems.
 *
 * @author Andy Wilkinson
 * @author Yanming Zhou
 * @author Scott Frederick
 * @author Ivan Malutin
 * @author Phillip Webb
 * @author Dmytro Nosan
 * @author Moritz Halbritter
 * @author Stefano Cordio
 */
public abstract class ArchitectureCheck extends DefaultTask {

	private static final String CONDITIONAL_ON_CLASS_ANNOTATION = "org.springframework.boot.autoconfigure.condition.ConditionalOnClass";

	private FileCollection classes;

	public ArchitectureCheck() {
		getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir(getName()));
		getConditionalOnClassAnnotation().convention(CONDITIONAL_ON_CLASS_ANNOTATION);
		getRules().addAll(getProhibitObjectsRequireNonNull().convention(true)
			.map(whenTrue(ArchitectureRules::noClassesShouldCallObjectsRequireNonNull)));
		getRules().addAll(ArchitectureRules.standard());
		getRules().addAll(whenMainSources(() -> List
			.of(ArchitectureRules.allBeanMethodsShouldReturnNonPrivateType(), ArchitectureRules
				.allBeanMethodsShouldNotHaveConditionalOnClassAnnotation(getConditionalOnClassAnnotation().get()))));
		getRules().addAll(whenMainSources(() -> Collections.singletonList(
				ArchitectureRules.allCustomAssertionMethodsNotReturningSelfShouldBeAnnotatedWithCheckReturnValue())));
		getRules().addAll(and(getNullMarkedEnabled(), isMainSourceSet()).map(whenTrue(() -> Collections.singletonList(
				ArchitectureRules.packagesShouldBeAnnotatedWithNullMarked(getNullMarkedIgnoredPackages().get())))));
		getRuleDescriptions().set(getRules().map(this::asDescriptions));
	}

	private Provider<Boolean> and(Provider<Boolean> provider1, Provider<Boolean> provider2) {
		return provider1.zip(provider2, (result1, result2) -> result1 && result2);
	}

	private Provider<List<ArchRule>> whenMainSources(Supplier<List<ArchRule>> rules) {
		return isMainSourceSet().map(whenTrue(rules));
	}

	private Provider<Boolean> isMainSourceSet() {
		return getSourceSet().convention(SourceSet.MAIN_SOURCE_SET_NAME).map(SourceSet.MAIN_SOURCE_SET_NAME::equals);
	}

	private Transformer<List<ArchRule>, Boolean> whenTrue(Supplier<List<ArchRule>> rules) {
		return (in) -> (!in) ? Collections.emptyList() : rules.get();
	}

	private List<String> asDescriptions(List<ArchRule> rules) {
		return rules.stream().map(ArchRule::getDescription).toList();
	}

	@TaskAction
	void checkArchitecture() throws Exception {
		withCompileClasspath(() -> {
			JavaClasses javaClasses = new ClassFileImporter().importPaths(classFilesPaths());
			List<EvaluationResult> results = new ArrayList<>();
			evaluate(javaClasses).forEach(results::add);
			results.add(new AutoConfigurationChecker().check(javaClasses));
			File outputFile = getOutputDirectory().file("failure-report.txt").get().getAsFile();
			List<EvaluationResult> violations = results.stream().filter(EvaluationResult::hasViolation).toList();
			writeViolationReport(violations, outputFile);
			if (!violations.isEmpty()) {
				throw new VerificationException("Architecture check failed. See '" + outputFile + "' for details.");
			}
			return null;
		});
	}

	private List<Path> classFilesPaths() {
		return this.classes.getFiles().stream().map(File::toPath).toList();
	}

	private Stream<EvaluationResult> evaluate(JavaClasses javaClasses) {
		return getRules().get().stream().map((rule) -> rule.evaluate(javaClasses));
	}

	private void withCompileClasspath(Callable<?> callable) throws Exception {
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		try {
			List<URL> urls = new ArrayList<>();
			for (File file : getCompileClasspath().getFiles()) {
				urls.add(file.toURI().toURL());
			}
			ClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
			Thread.currentThread().setContextClassLoader(classLoader);
			callable.call();
		}
		finally {
			Thread.currentThread().setContextClassLoader(previous);
		}
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

	@InputFiles
	@Classpath
	public abstract ConfigurableFileCollection getCompileClasspath();

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

	@Internal
	abstract Property<String> getSourceSet();

	@Input // Use descriptions as input since rules aren't serializable
	abstract ListProperty<String> getRuleDescriptions();

	@Internal
	abstract Property<Boolean> getNullMarkedEnabled();

	@Internal
	abstract SetProperty<String> getNullMarkedIgnoredPackages();

	@Internal
	abstract Property<String> getConditionalOnClassAnnotation();

}
