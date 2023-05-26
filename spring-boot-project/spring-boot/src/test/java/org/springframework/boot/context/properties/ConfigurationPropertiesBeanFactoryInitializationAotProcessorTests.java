/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.context.properties;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.ConfigurationPropertiesBeanFactoryInitializationAotProcessor.ConfigurationPropertiesReflectionHintsContribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigurationPropertiesBeanFactoryInitializationAotProcessor}.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @author Sebastien Deleuze
 */
class ConfigurationPropertiesBeanFactoryInitializationAotProcessorTests {

	private final ConfigurationPropertiesBeanFactoryInitializationAotProcessor processor = new ConfigurationPropertiesBeanFactoryInitializationAotProcessor();

	@Test
	void configurationPropertiesBeanFactoryInitializationAotProcessorIsRegistered() {
		assertThat(AotServices.factories().load(BeanFactoryInitializationAotProcessor.class))
			.anyMatch(ConfigurationPropertiesBeanFactoryInitializationAotProcessor.class::isInstance);
	}

	@Test
	void processNoMatchesReturnsNullContribution() {
		assertThat(process(String.class)).isNull();
	}

	@Test
	void processManuallyRegisteredSingleton() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("test", new SampleProperties());
		ConfigurationPropertiesReflectionHintsContribution contribution = process(beanFactory);
		assertThat(contribution.getTypes()).containsExactly(SampleProperties.class);
		assertThat(typeHints(contribution).map(TypeHint::getType))
			.containsExactly(TypeReference.of(SampleProperties.class));
	}

	@Test
	void processDefinedBean() {
		ConfigurationPropertiesReflectionHintsContribution contribution = process(SampleProperties.class);
		assertThat(contribution.getTypes()).containsExactly(SampleProperties.class);
		assertThat(typeHints(contribution).map(TypeHint::getType))
			.containsExactly(TypeReference.of(SampleProperties.class));
	}

	Stream<TypeHint> typeHints(ConfigurationPropertiesReflectionHintsContribution contribution) {
		GenerationContext generationContext = new TestGenerationContext();
		contribution.applyTo(generationContext, mock(BeanFactoryInitializationCode.class));
		return generationContext.getRuntimeHints().reflection().typeHints();
	}

	private ConfigurationPropertiesReflectionHintsContribution process(Class<?>... types) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		for (Class<?> type : types) {
			beanFactory.registerBeanDefinition(type.getName(), new RootBeanDefinition(type));
		}
		return process(beanFactory);
	}

	private ConfigurationPropertiesReflectionHintsContribution process(ConfigurableListableBeanFactory beanFactory) {
		return this.processor.processAheadOfTime(beanFactory);
	}

	@ConfigurationProperties("test")
	static class SampleProperties {

	}

}
