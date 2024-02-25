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
import java.net.URLDecoder;
import java.net.URLEncoder;
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
 * @author Yanming Zhou
 */
public abstract class ArchitectureCheck extends DefaultTask {

	private FileCollection classes;

	/**
	 * This method initializes the architecture check for the project. It sets the output
	 * directory convention to the build directory of the project. It adds various rules
	 * to be checked for the architecture of the project. The rule descriptions are set
	 * based on the rules added.
	 */
	public ArchitectureCheck() {
		getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir(getName()));
		getRules().addAll(allPackagesShouldBeFreeOfTangles(),
				allBeanPostProcessorBeanMethodsShouldBeStaticAndHaveParametersThatWillNotCausePrematureInitialization(),
				allBeanFactoryPostProcessorBeanMethodsShouldBeStaticAndHaveNoParameters(),
				noClassesShouldCallStepVerifierStepVerifyComplete(),
				noClassesShouldConfigureDefaultStepVerifierTimeout(), noClassesShouldCallCollectorsToList(),
				noClassesShouldCallURLEncoderWithStringEncoding(), noClassesShouldCallURLDecoderWithStringEncoding());
		getRuleDescriptions().set(getRules().map((rules) -> rules.stream().map(ArchRule::getDescription).toList()));
	}

	/**
	 * Checks the architecture of the project.
	 * @throws IOException if an I/O error occurs while importing class files
	 * @return void
	 */
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
				report.append(violation.getFailureReport());
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

	/**
	 * This method defines an architectural rule that checks if all packages in the
	 * project are free of tangles. A tangle occurs when there is a cyclic dependency
	 * between packages. The rule uses the ArchUnit library to define and enforce the
	 * architectural constraint. It uses the SlicesRuleDefinition to define slices of
	 * packages and checks if there are any cycles within them.
	 * @return An ArchRule object representing the architectural rule.
	 */
	private ArchRule allPackagesShouldBeFreeOfTangles() {
		return SlicesRuleDefinition.slices().matching("(**)").should().beFreeOfCycles();
	}

	/**
	 * Returns an ArchRule that checks if all methods annotated with "@Bean" in the
	 * ArchitectureCheck class that have a raw return type assignable to
	 * "org.springframework.beans.factory.config.BeanPostProcessor" only have parameters
	 * that will not cause premature initialization. The methods should also be static.
	 * @return the ArchRule for checking the above conditions
	 */
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

	/**
	 * Returns an ArchCondition that checks if a JavaMethod only has parameters that will
	 * not cause eager initialization.
	 * @return the ArchCondition that checks for parameters that will not cause eager
	 * initialization
	 */
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

	/**
	 * This method defines an architectural rule that enforces all methods annotated with
	 * "@Bean" in the Spring framework should be static and have no parameters. This rule
	 * specifically targets methods that have a return type assignable to
	 * "BeanFactoryPostProcessor" in the "org.springframework.beans.factory.config"
	 * package.
	 * @return An ArchRule object representing the defined architectural rule.
	 */
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

	/**
	 * Returns an ArchCondition that checks if a JavaMethod has no parameters.
	 * @return the ArchCondition that checks for no parameters
	 */
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

	/**
	 * This method defines an ArchRule that enforces that no classes should call the
	 * method "verifyComplete()" from the "StepVerifier$Step" class.
	 *
	 * The reason for this rule is that calling "verifyComplete()" can block indefinitely,
	 * and it is recommended to use "expectComplete().verify(Duration)" instead.
	 * @return The ArchRule object that enforces the
	 * noClassesShouldCallStepVerifierStepVerifyComplete rule.
	 */
	private ArchRule noClassesShouldCallStepVerifierStepVerifyComplete() {
		return ArchRuleDefinition.noClasses()
			.should()
			.callMethod("reactor.test.StepVerifier$Step", "verifyComplete")
			.because("it can block indefinitely and expectComplete().verify(Duration) should be used instead");
	}

	/**
	 * This method defines an architectural rule that states that no classes should
	 * configure the default timeout for StepVerifier. Instead, the
	 * expectComplete().verify(Duration) method should be used.
	 * @return An ArchRule object representing the architectural rule.
	 */
	private ArchRule noClassesShouldConfigureDefaultStepVerifierTimeout() {
		return ArchRuleDefinition.noClasses()
			.should()
			.callMethod("reactor.test.StepVerifier", "setDefaultTimeout", "java.time.Duration")
			.because("expectComplete().verify(Duration) should be used instead");
	}

	/**
	 * This method defines an architectural rule that states no classes should call the
	 * method Collectors.toList(). Instead, the method java.util.stream.Stream.toList()
	 * should be used.
	 * @return An ArchRule object representing the defined architectural rule.
	 */
	private ArchRule noClassesShouldCallCollectorsToList() {
		return ArchRuleDefinition.noClasses()
			.should()
			.callMethod(Collectors.class, "toList")
			.because("java.util.stream.Stream.toList() should be used instead");
	}

	/**
	 * This method defines an architectural rule that no classes should call the
	 * deprecated method URLEncoder.encode(String s, String encoding). Instead, the
	 * recommended method URLEncoder.encode(String s, Charset charset) should be used.
	 * @return ArchRule - The architectural rule that no classes should call
	 * URLEncoder.encode(String s, String encoding).
	 * @since 1.0
	 * @see java.net.URLEncoder#encode(String s, Charset charset)
	 */
	private ArchRule noClassesShouldCallURLEncoderWithStringEncoding() {
		return ArchRuleDefinition.noClasses()
			.should()
			.callMethod(URLEncoder.class, "encode", String.class, String.class)
			.because("java.net.URLEncoder.encode(String s, Charset charset) should be used instead");
	}

	/**
	 * This method defines an architectural rule that no classes should call the
	 * URLDecoder.decode() method with String encoding. Instead, the
	 * java.net.URLDecoder.decode(String s, Charset charset) method should be used.
	 * @return An ArchRule object representing the architectural rule.
	 */
	private ArchRule noClassesShouldCallURLDecoderWithStringEncoding() {
		return ArchRuleDefinition.noClasses()
			.should()
			.callMethod(URLDecoder.class, "decode", String.class, String.class)
			.because("java.net.URLDecoder.decode(String s, Charset charset) should be used instead");
	}

	/**
	 * Sets the classes for the ArchitectureCheck.
	 * @param classes the FileCollection containing the classes to be set
	 */
	public void setClasses(FileCollection classes) {
		this.classes = classes;
	}

	/**
	 * Returns the collection of classes.
	 * @return the collection of classes
	 */
	@Internal
	public FileCollection getClasses() {
		return this.classes;
	}

	/**
	 * Returns the input classes as a file tree.
	 *
	 * This method skips when the input is empty and ignores empty directories. The path
	 * sensitivity is set to relative.
	 * @return the input classes as a file tree
	 */
	@InputFiles
	@SkipWhenEmpty
	@IgnoreEmptyDirectories
	@PathSensitive(PathSensitivity.RELATIVE)
	final FileTree getInputClasses() {
		return this.classes.getAsFileTree();
	}

	/**
	 * Returns the directory property for the resources directory.
	 * @return The directory property for the resources directory.
	 */
	@Optional
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract DirectoryProperty getResourcesDirectory();

	/**
	 * Returns the output directory.
	 * @return the output directory
	 */
	@OutputDirectory
	public abstract DirectoryProperty getOutputDirectory();

	/**
	 * Returns the list of architectural rules.
	 * @return the list of architectural rules
	 */
	@Internal
	public abstract ListProperty<ArchRule> getRules();

	/**
	 * Returns the descriptions of the rules.
	 * @return the descriptions of the rules
	 */
	@Input
	// The rules themselves can't be an input as they aren't serializable so we use their
	// descriptions instead
	abstract ListProperty<String> getRuleDescriptions();

}
