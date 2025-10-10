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

package org.springframework.boot.logging.logback;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.model.RootLoggerModel;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.model.ComponentModel;
import ch.qos.logback.core.model.ImplicitModel;
import ch.qos.logback.core.model.ImportModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.TimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.JavaSerializationHint;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.SerializationHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.logback.SpringBootJoranConfigurator.LogbackConfigurationAotContribution;
import org.springframework.context.aot.AbstractAotProcessor;
import org.springframework.core.io.InputStreamSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LogbackConfigurationAotContribution}.
 *
 * @author Andy Wilkinson
 */
class LogbackConfigurationAotContributionTests {

	@BeforeEach
	@AfterEach
	void prepare() {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();
	}

	@Test
	void contributionOfBasicModel() {
		TestGenerationContext generationContext = applyContribution(new Model());
		InMemoryGeneratedFiles generatedFiles = generationContext.getGeneratedFiles();
		assertThat(generatedFiles).has(resource("META-INF/spring/logback-model"));
		assertThat(generatedFiles).has(resource("META-INF/spring/logback-pattern-rules"));
		SerializationHints serializationHints = generationContext.getRuntimeHints().serialization();
		assertThat(serializationHints.javaSerializationHints()
			.map(JavaSerializationHint::getType)
			.map(TypeReference::getName))
			.containsExactlyInAnyOrder(namesOf(Model.class, ArrayList.class, Boolean.class, Integer.class));
		assertThat(generationContext.getRuntimeHints().reflection().typeHints()).isEmpty();
		InputStreamSource generatedFile = generatedFiles.getGeneratedFile(Kind.RESOURCE,
				"META-INF/spring/logback-pattern-rules");
		assertThat(generatedFile).isNotNull();
		Properties patternRules = load(generatedFile);
		assertThat(patternRules).isEmpty();
	}

	@Test
	void contributionOfBasicModelThatMatchesExistingModel() {
		TestGenerationContext generationContext = new TestGenerationContext();
		Model model = new Model();
		applyContribution(model, generationContext);
		applyContribution(model, generationContext);
		InMemoryGeneratedFiles generatedFiles = generationContext.getGeneratedFiles();
		assertThat(generatedFiles).has(resource("META-INF/spring/logback-model"));
		assertThat(generatedFiles).has(resource("META-INF/spring/logback-pattern-rules"));
		SerializationHints serializationHints = generationContext.getRuntimeHints().serialization();
		assertThat(serializationHints.javaSerializationHints()
			.map(JavaSerializationHint::getType)
			.map(TypeReference::getName))
			.containsExactlyInAnyOrder(namesOf(Model.class, ArrayList.class, Boolean.class, Integer.class));
		assertThat(generationContext.getRuntimeHints().reflection().typeHints()).isEmpty();
		InputStreamSource generatedFile = generatedFiles.getGeneratedFile(Kind.RESOURCE,
				"META-INF/spring/logback-pattern-rules");
		assertThat(generatedFile).isNotNull();
		Properties patternRules = load(generatedFile);
		assertThat(patternRules).isEmpty();
	}

	@Test
	void contributionOfBasicModelThatDiffersFromExistingModelThrows() {
		TestGenerationContext generationContext = new TestGenerationContext();
		applyContribution(new Model(), generationContext);
		Model model = new Model();
		model.addSubModel(new RootLoggerModel());
		assertThatIllegalStateException().isThrownBy(() -> applyContribution(model, generationContext))
			.withMessage("Logging configuration differs from the configuration that has already been written. "
					+ "Update your logging configuration so that it is the same for each context");
	}

	@Test
	void patternRulesAreStoredAndRegisteredForReflection() {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.putObject(CoreConstants.PATTERN_RULE_REGISTRY,
				Map.of("a", "com.example.Alpha", "b", "com.example.Bravo"));
		TestGenerationContext generationContext = applyContribution(new Model());
		assertThat(invokePublicConstructorsOf("com.example.Alpha")).accepts(generationContext.getRuntimeHints());
		assertThat(invokePublicConstructorsOf("com.example.Bravo")).accepts(generationContext.getRuntimeHints());
		InputStreamSource generatedFile = generationContext.getGeneratedFiles()
			.getGeneratedFile(Kind.RESOURCE, "META-INF/spring/logback-pattern-rules");
		assertThat(generatedFile).isNotNull();
		Properties patternRules = load(generatedFile);
		assertThat(patternRules).hasSize(2);
		assertThat(patternRules).containsEntry("a", "com.example.Alpha");
		assertThat(patternRules).containsEntry("b", "com.example.Bravo");
	}

	@Test
	void componentModelClassAndSetterParametersAreRegisteredForReflection() {
		ComponentModel component = new ComponentModel();
		component.setClassName(SizeAndTimeBasedRollingPolicy.class.getName());
		Model model = new Model();
		model.getSubModels().add(component);
		TestGenerationContext generationContext = applyContribution(model);
		RuntimeHints runtimeHints = generationContext.getRuntimeHints();
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(SizeAndTimeBasedRollingPolicy.class))
			.accepts(runtimeHints);
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(FileAppender.class))
			.accepts(runtimeHints);
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(FileSize.class)).accepts(runtimeHints);
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(
				TimeBasedFileNamingAndTriggeringPolicy.class))
			.accepts(runtimeHints);
		assertThat(runtimeHints).satisfies(hasValidTypeName(SizeAndTimeBasedRollingPolicy.class));
		assertThat(runtimeHints).satisfies(hasValidTypeName(FileSize.class));
		assertThat(runtimeHints).satisfies(hasValidTypeName(FileAppender.class));
	}

	@Test
	void implicitModelClassAndSetterParametersAreRegisteredForReflection() {
		ImplicitModel implicit = new ImplicitModel();
		implicit.setTag("encoder");
		Model model = new Model();
		model.getSubModels().add(implicit);
		TestGenerationContext generationContext = applyContribution(model);
		RuntimeHints runtimeHints = generationContext.getRuntimeHints();
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(PatternLayoutEncoder.class))
			.accepts(runtimeHints);
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Layout.class)).accepts(runtimeHints);
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Charset.class)).accepts(runtimeHints);
		assertThat(runtimeHints).satisfies(hasValidTypeName(PatternLayoutEncoder.class));
		assertThat(runtimeHints).satisfies(hasValidTypeName(Layout.class));
		assertThat(runtimeHints).satisfies(hasValidTypeName(Charset.class));
	}

	@Test
	void componentModelReferencingImportedClassNameIsRegisteredForReflection() {
		ImportModel importModel = new ImportModel();
		importModel.setClassName(SizeAndTimeBasedRollingPolicy.class.getName());
		ComponentModel component = new ComponentModel();
		component.setClassName(SizeAndTimeBasedRollingPolicy.class.getSimpleName());
		Model model = new Model();
		model.getSubModels().addAll(List.of(importModel, component));
		TestGenerationContext generationContext = applyContribution(model);
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(SizeAndTimeBasedRollingPolicy.class))
			.accepts(generationContext.getRuntimeHints());
		assertThat(generationContext.getRuntimeHints())
			.satisfies(hasValidTypeName(SizeAndTimeBasedRollingPolicy.class));
	}

	@Test
	void typeFromParentsSetterIsRegisteredForReflection() {
		ImplicitModel implementation = new ImplicitModel();
		implementation.setTag("implementation");
		ComponentModel component = new ComponentModel();
		component.setClassName(Outer.class.getName());
		component.getSubModels().add(implementation);
		TestGenerationContext generationContext = applyContribution(component);
		RuntimeHints runtimeHints = generationContext.getRuntimeHints();
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Outer.class)).accepts(runtimeHints);
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Implementation.class))
			.accepts(runtimeHints);
		assertThat(runtimeHints).satisfies(hasValidTypeName(Outer.class));
		assertThat(runtimeHints).satisfies(hasValidTypeName(Implementation.class));
	}

	@Test
	void typeFromParentsDefaultClassAnnotatedSetterIsRegisteredForReflection() {
		ImplicitModel contract = new ImplicitModel();
		contract.setTag("contract");
		ComponentModel component = new ComponentModel();
		component.setClassName(OuterWithDefaultClass.class.getName());
		component.getSubModels().add(contract);
		TestGenerationContext generationContext = applyContribution(component);
		RuntimeHints runtimeHints = generationContext.getRuntimeHints();
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(OuterWithDefaultClass.class))
			.accepts(runtimeHints);
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Implementation.class))
			.accepts(runtimeHints);
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(BaseImplementation.Details.class))
			.accepts(runtimeHints);
		assertThat(runtimeHints).satisfies(hasValidTypeName(OuterWithDefaultClass.class));
		assertThat(runtimeHints).satisfies(hasValidTypeName(Implementation.class));
		assertThat(runtimeHints).satisfies(hasValidTypeName(BaseImplementation.Details.class));
	}

	@Test
	void componentTypesOfArraysAreRegisteredForReflection() {
		ComponentModel component = new ComponentModel();
		component.setClassName(ArrayParameters.class.getName());
		TestGenerationContext generationContext = applyContribution(component);
		RuntimeHints runtimeHints = generationContext.getRuntimeHints();
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(InetSocketAddress.class))
			.accepts(runtimeHints);
		assertThat(runtimeHints).satisfies(hasValidTypeName(InetSocketAddress.class));
		assertThat(runtimeHints).satisfies(hasValidTypeName(ArrayParameters.class));
	}

	@Test
	void placeholdersInComponentClassAttributeAreReplaced() {
		ComponentModel component = new ComponentModel();
		component.setClassName("${VARIABLE_CLASS_NAME}");
		TestGenerationContext generationContext = applyContribution(component,
				(context) -> context.putProperty("VARIABLE_CLASS_NAME", Outer.class.getName()));
		RuntimeHints runtimeHints = generationContext.getRuntimeHints();
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Outer.class)).accepts(runtimeHints);
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Implementation.class))
			.accepts(runtimeHints);
		assertThat(runtimeHints).satisfies(hasValidTypeName(Outer.class));
		assertThat(runtimeHints).satisfies(hasValidTypeName(Implementation.class));
	}

	private Predicate<RuntimeHints> invokePublicConstructorsOf(String name) {
		return RuntimeHintsPredicates.reflection()
			.onType(TypeReference.of(name))
			.withMemberCategory(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
	}

	private Predicate<RuntimeHints> invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Class<?> type) {
		return RuntimeHintsPredicates.reflection()
			.onType(TypeReference.of(type))
			.withMemberCategories(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);
	}

	private Consumer<RuntimeHints> hasValidTypeName(Class<?> type) {
		return (runtimeHints) -> assertThat(runtimeHints.reflection().getTypeHint(type)).extracting(TypeHint::getType)
			.extracting(TypeReference::getName)
			.isEqualTo(type.getName());
	}

	private Properties load(InputStreamSource source) {
		try (InputStream inputStream = source.getInputStream()) {
			Properties properties = new Properties();
			properties.load(inputStream);
			return properties;
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private Condition<InMemoryGeneratedFiles> resource(String name) {
		return new Condition<>((files) -> files.getGeneratedFile(Kind.RESOURCE, name) != null,
				"has a resource named '%s'", name);
	}

	private TestGenerationContext applyContribution(Model model) {
		return this.applyContribution(model, (context) -> {
		});
	}

	private TestGenerationContext applyContribution(Model model, Consumer<LoggerContext> contextCustomizer) {
		TestGenerationContext generationContext = new TestGenerationContext();
		applyContribution(model, contextCustomizer, generationContext);
		return generationContext;
	}

	private void applyContribution(Model model, TestGenerationContext generationContext) {
		applyContribution(model, (context) -> {
		}, generationContext);
	}

	private void applyContribution(Model model, Consumer<LoggerContext> contextCustomizer,
			TestGenerationContext generationContext) {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		contextCustomizer.accept(context);
		SpringBootJoranConfigurator configurator = new SpringBootJoranConfigurator(
				new LoggingInitializationContext(null));
		configurator.setContext(context);
		withSystemProperty(AbstractAotProcessor.AOT_PROCESSING, "true", () -> configurator.processModel(model));
		LogbackConfigurationAotContribution contribution = (LogbackConfigurationAotContribution) context
			.getObject(BeanFactoryInitializationAotContribution.class.getName());
		contribution.applyTo(generationContext, mock(BeanFactoryInitializationCode.class));
	}

	private String[] namesOf(Class<?>... types) {
		return Stream.of(types).map(Class::getName).toArray(String[]::new);
	}

	private void withSystemProperty(String name, String value, Runnable action) {
		System.setProperty(name, value);
		try {
			action.run();
		}
		finally {
			System.clearProperty(name);
		}
	}

	public static class Outer {

		public void setImplementation(Implementation implementation) {
		}

	}

	public static class OuterWithDefaultClass {

		@DefaultClass(Implementation.class)
		public void setContract(Contract contract) {
		}

	}

	public static class BaseImplementation implements Contract {

		public void setDetails(Details details) {
		}

		public static final class Details {

		}

	}

	public static class Implementation extends BaseImplementation {

	}

	public interface Contract {

	}

	public static class ArrayParameters {

		public void addDestinations(InetSocketAddress... addresses) {

		}

	}

}
