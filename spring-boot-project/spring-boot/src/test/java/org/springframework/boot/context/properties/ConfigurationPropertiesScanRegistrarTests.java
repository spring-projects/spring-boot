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
import org.springframework.boot.context.properties.scan.ConfigurationPropertiesScanConfiguration;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesScanRegistrar}.
 *
 * @author Madhura Bhave
 */
public class ConfigurationPropertiesScanRegistrarTests {

	private final ConfigurationPropertiesScanRegistrar registrar = new ConfigurationPropertiesScanRegistrar();

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@Test
	public void registerBeanDefintionsShouldScanForConfigurationProperties()
			throws IOException {
		this.registrar.registerBeanDefinitions(
				getAnnotationMetadata(ConfigurationPropertiesScanConfiguration.class),
				this.beanFactory);
		BeanDefinition bingDefinition = this.beanFactory.getBeanDefinition(
				"bing-org.springframework.boot.context.properties.scan.ConfigurationPropertiesScanConfiguration$BingProperties");
		BeanDefinition fooDefinition = this.beanFactory.getBeanDefinition(
				"foo-org.springframework.boot.context.properties.scan.ConfigurationPropertiesScanConfiguration$FooProperties");
		BeanDefinition barDefinition = this.beanFactory.getBeanDefinition(
				"bar-org.springframework.boot.context.properties.scan.ConfigurationPropertiesScanConfiguration$BarProperties");
		assertThat(bingDefinition).isExactlyInstanceOf(GenericBeanDefinition.class);
		assertThat(fooDefinition).isExactlyInstanceOf(GenericBeanDefinition.class);
		assertThat(barDefinition)
				.isExactlyInstanceOf(ConfigurationPropertiesBeanDefinition.class);
	}

	@Test
	public void scanWhenBeanDefinitionExistsShouldSkip() throws IOException {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.setAllowBeanDefinitionOverriding(false);
		this.registrar.registerBeanDefinitions(
				getAnnotationMetadata(
						ConfigurationPropertiesScanConfiguration.TestConfiguration.class),
				beanFactory);
		BeanDefinition fooDefinition = beanFactory.getBeanDefinition(
				"foo-org.springframework.boot.context.properties.scan.ConfigurationPropertiesScanConfiguration$FooProperties");
		assertThat(fooDefinition).isExactlyInstanceOf(GenericBeanDefinition.class);
	}

	@Test
	public void scanWhenBasePackagesAndBasePackcageClassesProvidedShouldUseThat()
			throws IOException {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.setAllowBeanDefinitionOverriding(false);
		this.registrar.registerBeanDefinitions(getAnnotationMetadata(
				ConfigurationPropertiesScanConfiguration.DifferentPackageConfiguration.class),
				beanFactory);
		assertThat(beanFactory.containsBeanDefinition(
				"foo-org.springframework.boot.context.properties.scan.ConfigurationPropertiesScanConfiguration$FooProperties"))
						.isFalse();
		BeanDefinition aDefinition = beanFactory.getBeanDefinition(
				"a-org.springframework.boot.context.properties.scan.a.AScanConfiguration$AProperties");
		BeanDefinition bDefinition = beanFactory.getBeanDefinition(
				"b-org.springframework.boot.context.properties.scan.b.BScanConfiguration$BProperties");
		assertThat(aDefinition).isExactlyInstanceOf(GenericBeanDefinition.class);
		assertThat(bDefinition).isExactlyInstanceOf(GenericBeanDefinition.class);
	}

	private AnnotationMetadata getAnnotationMetadata(Class<?> source) throws IOException {
		return new SimpleMetadataReaderFactory().getMetadataReader(source.getName())
				.getAnnotationMetadata();
	}

}
