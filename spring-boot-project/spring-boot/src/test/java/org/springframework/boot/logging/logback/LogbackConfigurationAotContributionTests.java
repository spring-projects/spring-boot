/*
 * Copyright 2012-2022 the original author or authors.
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Stream;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.Layout;
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
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.boot.logging.logback.SpringBootJoranConfigurator.LogbackConfigurationAotContribution;
import org.springframework.core.io.InputStreamSource;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(serializationHints.javaSerializationHints().map(JavaSerializationHint::getType)
				.map(TypeReference::getName))
						.containsExactlyInAnyOrder(namesOf(Model.class, ArrayList.class, Boolean.class, Integer.class));
		assertThat(generationContext.getRuntimeHints().reflection().typeHints()).isEmpty();
		Properties patternRules = load(
				generatedFiles.getGeneratedFile(Kind.RESOURCE, "META-INF/spring/logback-pattern-rules"));
		assertThat(patternRules).isEmpty();
	}

	@Test
	void patternRulesAreStoredAndRegisteredForReflection() {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.putObject(CoreConstants.PATTERN_RULE_REGISTRY,
				Map.of("a", "com.example.Alpha", "b", "com.example.Bravo"));
		TestGenerationContext generationContext = applyContribution(new Model());
		assertThat(invokePublicConstructorsOf("com.example.Alpha")).accepts(generationContext.getRuntimeHints());
		assertThat(invokePublicConstructorsOf("com.example.Bravo")).accepts(generationContext.getRuntimeHints());
		Properties patternRules = load(generationContext.getGeneratedFiles().getGeneratedFile(Kind.RESOURCE,
				"META-INF/spring/logback-pattern-rules"));
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
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(SizeAndTimeBasedRollingPolicy.class))
				.accepts(generationContext.getRuntimeHints());
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(FileAppender.class))
				.accepts(generationContext.getRuntimeHints());
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(FileSize.class))
				.accepts(generationContext.getRuntimeHints());
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(
				TimeBasedFileNamingAndTriggeringPolicy.class)).accepts(generationContext.getRuntimeHints());
	}

	@Test
	void implicitModelClassAndSetterParametersAreRegisteredForReflection() {
		ImplicitModel implicit = new ImplicitModel();
		implicit.setTag("encoder");
		Model model = new Model();
		model.getSubModels().add(implicit);
		TestGenerationContext generationContext = applyContribution(model);
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(PatternLayoutEncoder.class))
				.accepts(generationContext.getRuntimeHints());
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Layout.class))
				.accepts(generationContext.getRuntimeHints());
		assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Charset.class))
				.accepts(generationContext.getRuntimeHints());
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
	}

	private Predicate<RuntimeHints> invokePublicConstructorsOf(String name) {
		return RuntimeHintsPredicates.reflection().onType(TypeReference.of(name))
				.withMemberCategory(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
	}

	private Predicate<RuntimeHints> invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Class<?> type) {
		return RuntimeHintsPredicates.reflection().onType(TypeReference.of(type)).withMemberCategories(
				MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INTROSPECT_PUBLIC_METHODS,
				MemberCategory.INVOKE_PUBLIC_METHODS);
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
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		SpringBootJoranConfigurator configurator = new SpringBootJoranConfigurator(null);
		configurator.setContext(context);
		withSystemProperty("spring.aot.processing", "true", () -> configurator.processModel(model));
		LogbackConfigurationAotContribution contribution = (LogbackConfigurationAotContribution) context
				.getObject(BeanFactoryInitializationAotContribution.class.getName());
		TestGenerationContext generationContext = new TestGenerationContext();
		contribution.applyTo(generationContext, null);
		return generationContext;
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

}
