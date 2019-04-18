/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.IOException;

import org.junit.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link EnableConfigurationPropertiesImportSelector}.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 */
public class EnableConfigurationPropertiesImportSelectorTests {

	private final EnableConfigurationPropertiesImportSelector importSelector = new EnableConfigurationPropertiesImportSelector();

	private final EnableConfigurationPropertiesImportSelector.ConfigurationPropertiesBeanRegistrar registrar = new EnableConfigurationPropertiesImportSelector.ConfigurationPropertiesBeanRegistrar();

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@Test
	public void selectImports() {
		String[] imports = this.importSelector
				.selectImports(mock(AnnotationMetadata.class));
		assertThat(imports).containsExactly(
				EnableConfigurationPropertiesImportSelector.ConfigurationPropertiesBeanRegistrar.class
						.getName(),
				ConfigurationPropertiesBindingPostProcessorRegistrar.class.getName());
	}

	@Test
	public void typeWithDefaultConstructorShouldRegisterGenericBeanDefinition()
			throws Exception {
		this.registrar.registerBeanDefinitions(
				getAnnotationMetadata(TestConfiguration.class), this.beanFactory);
		BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition(
				"foo-org.springframework.boot.context.properties.EnableConfigurationPropertiesImportSelectorTests$FooProperties");
		assertThat(beanDefinition).isExactlyInstanceOf(GenericBeanDefinition.class);
	}

	@Test
	public void typeWithOneConstructorWithParametersShouldRegisterConfigurationPropertiesBeanDefinition()
			throws Exception {
		this.registrar.registerBeanDefinitions(
				getAnnotationMetadata(TestConfiguration.class), this.beanFactory);
		BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition(
				"bar-org.springframework.boot.context.properties.EnableConfigurationPropertiesImportSelectorTests$BarProperties");
		assertThat(beanDefinition)
				.isExactlyInstanceOf(ConfigurationPropertiesBeanDefinition.class);
	}

	@Test
	public void typeWithMultipleConstructorsShouldRegisterGenericBeanDefinition()
			throws Exception {
		this.registrar.registerBeanDefinitions(
				getAnnotationMetadata(TestConfiguration.class), this.beanFactory);
		BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition(
				"bing-org.springframework.boot.context.properties.EnableConfigurationPropertiesImportSelectorTests$BingProperties");
		assertThat(beanDefinition).isExactlyInstanceOf(GenericBeanDefinition.class);
	}

	@Test
	public void typeWithNoAnnotationShouldFail() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.registrar.registerBeanDefinitions(
						getAnnotationMetadata(InvalidConfiguration.class),
						this.beanFactory))
				.withMessageContaining("No ConfigurationProperties annotation found")
				.withMessageContaining(
						EnableConfigurationPropertiesImportSelectorTests.class.getName());
	}

	@Test
	public void registrationWithDuplicatedTypeShouldRegisterSingleBeanDefinition()
			throws IOException {
		DefaultListableBeanFactory factory = spy(this.beanFactory);
		this.registrar.registerBeanDefinitions(
				getAnnotationMetadata(DuplicateConfiguration.class), factory);
		verify(factory, times(1)).registerBeanDefinition(anyString(), any());
	}

	@Test
	public void registrationWithNoTypeShouldNotRegisterAnything() throws IOException {
		DefaultListableBeanFactory factory = spy(this.beanFactory);
		this.registrar.registerBeanDefinitions(
				getAnnotationMetadata(EmptyConfiguration.class), factory);
		verifyZeroInteractions(factory);
	}

	private AnnotationMetadata getAnnotationMetadata(Class<?> source) throws IOException {
		return new SimpleMetadataReaderFactory().getMetadataReader(source.getName())
				.getAnnotationMetadata();
	}

	@EnableConfigurationProperties({ FooProperties.class, BarProperties.class,
			BingProperties.class })
	static class TestConfiguration {

	}

	@EnableConfigurationProperties(EnableConfigurationPropertiesImportSelectorTests.class)
	static class InvalidConfiguration {

	}

	@EnableConfigurationProperties({ FooProperties.class, FooProperties.class })
	static class DuplicateConfiguration {

	}

	@EnableConfigurationProperties
	static class EmptyConfiguration {

	}

	@ConfigurationProperties(prefix = "foo")
	static class FooProperties {

	}

	@ConfigurationProperties(prefix = "bar")
	public static class BarProperties {

		public BarProperties(String foo) {

		}

	}

	@ConfigurationProperties(prefix = "bing")
	public static class BingProperties {

		public BingProperties() {

		}

		public BingProperties(String foo) {

		}

	}

}
