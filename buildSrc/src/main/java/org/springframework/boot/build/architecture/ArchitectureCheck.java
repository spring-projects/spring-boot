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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClass.Predicates;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.ListProperty;
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
 */
public abstract class ArchitectureCheck extends DefaultTask {

	private FileCollection classes;

	public ArchitectureCheck() {
		getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir(getName()));
		getRules().addAll(allPackagesShouldBeFreeOfTangles(),
				allBeanPostProcessorBeanMethodsShouldBeStaticAndHaveParametersThatWillNotCausePrematureInitialization(),
				allBeanFactoryPostProcessorBeanMethodsShouldBeStaticAndHaveNoParameters(),
				noClassesShouldCallStepVerifierStepVerifyComplete(),
				noClassesShouldConfigureDefaultStepVerifierTimeout(), noClassesShouldCallCollectorsToList());
		getRuleDescriptions().set(getRules().map((rules) -> rules.stream().map(ArchRule::getDescription).toList()));
	}

	@TaskAction
	void checkArchitecture() throws IOException {
		JavaClasses javaClasses = new ClassFileImporter()
			.importPaths(this.classes.getFiles().stream().map(File::toPath).toList());
		List<EvaluationResult> violations = getRules().get()
			.stream()
			.map((rule) -> rule.evaluate(javaClasses))
			.filter(EvaluationResult::hasViolation)
			.toList();
		File outputFile = getOutputDirectory().file("failure-report.txt").get().getAsFile();
		outputFile.getParentFile().mkdirs();
		if (!violations.isEmpty()) {
			StringBuilder report = new StringBuilder();
			for (EvaluationResult violation : violations) {
				report.append(violation.getFailureReport().toString());
				report.append(String.format("%n"));
			}
			Files.writeString(outputFile.toPath(), report.toString(), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
			throw new GradleException("Architecture check failed. See '" + outputFile + "' for details.");
		}
		else {
			outputFile.createNewFile();
		}
	}

	private ArchRule allPackagesShouldBeFreeOfTangles() {
		return SlicesRuleDefinition.slices().matching("(**)").should().beFreeOfCycles();
	}

	private ArchRule allBeanPostProcessorBeanMethodsShouldBeStaticAndHaveParametersThatWillNotCausePrematureInitialization() {
		return ArchRuleDefinition.methods()
			.that()
			.areAnnotatedWith("org.springframework.context.annotation.Bean")
			.and()
			.haveRawReturnType(Predicates.assignableTo("org.springframework.beans.factory.config.BeanPostProcessor"))
			.should(onlyHaveParametersThatWillNotCauseEagerInitialization())
			.andShould()
			.beStatic()
			.allowEmptyShould(true);
	}

	private ArchCondition<JavaMethod> onlyHaveParametersThatWillNotCauseEagerInitialization() {
		DescribedPredicate<CanBeAnnotated> notAnnotatedWithLazy = DescribedPredicate
			.not(CanBeAnnotated.Predicates.annotatedWith("org.springframework.context.annotation.Lazy"));
		DescribedPredicate<JavaClass> notOfASafeType = DescribedPredicate
			.not(Predicates.assignableTo("org.springframework.beans.factory.ObjectProvider")
				.or(Predicates.assignableTo("org.springframework.context.ApplicationContext"))
				.or(Predicates.assignableTo("org.springframework.core.env.Environment")));
		return new ArchCondition<>("not have parameters that will cause eager initialization") {

			@Override
			public void check(JavaMethod item, ConditionEvents events) {
				item.getParameters()
					.stream()
					.filter(notAnnotatedWithLazy)
					.filter((parameter) -> notOfASafeType.test(parameter.getRawType()))
					.forEach((parameter) -> events.add(SimpleConditionEvent.violated(parameter,
							parameter.getDescription() + " will cause eager initialization as it is "
									+ notAnnotatedWithLazy.getDescription() + " and is "
									+ notOfASafeType.getDescription())));
			}

		};
	}

	private ArchRule allBeanFactoryPostProcessorBeanMethodsShouldBeStaticAndHaveNoParameters() {
		return ArchRuleDefinition.methods()
			.that()
			.areAnnotatedWith("org.springframework.context.annotation.Bean")
			.and()
			.haveRawReturnType(
					Predicates.assignableTo("org.springframework.beans.factory.config.BeanFactoryPostProcessor"))
			.should(haveNoParameters())
			.andShould()
			.beStatic()
			.allowEmptyShould(true);
	}

	private ArchCondition<JavaMethod> haveNoParameters() {
		return new ArchCondition<>("have no parameters") {

			@Override
			public void check(JavaMethod item, ConditionEvents events) {
				List<JavaParameter> parameters = item.getParameters();
				if (!parameters.isEmpty()) {
					events
						.add(SimpleConditionEvent.violated(item, item.getDescription() + " should have no parameters"));
				}
			}

		};
	}

	private ArchRule noClassesShouldCallStepVerifierStepVerifyComplete() {
		return ArchRuleDefinition.noClasses()
			.should()
			.callMethod("reactor.test.StepVerifier$Step", "verifyComplete")
			.because("it can block indefinitely and expectComplete().verify(Duration) should be used instead");
	}

	private ArchRule noClassesShouldConfigureDefaultStepVerifierTimeout() {
		return ArchRuleDefinition.noClasses()
			.should()
			.callMethod("reactor.test.StepVerifier", "setDefaultTimeout", "java.time.Duration")
			.because("expectComplete().verify(Duration) should be used instead");
	}

	private ArchRule noClassesShouldCallCollectorsToList() {
		return ArchRuleDefinition.noClasses()
			.should()
			.callMethod(Collectors.class, "toList")
			.because("java.util.stream.Stream.toList() should be used instead");
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

	@Input
	// The rules themselves can't be an input as they aren't serializable so we use their
	// descriptions instead
	abstract ListProperty<String> getRuleDescriptions();

}
