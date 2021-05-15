/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SharedMetadataReaderFactoryContextInitializer.CachingMetadataReaderFactoryPostProcessor;
import org.springframework.boot.type.classreading.ConcurrentReferenceCachingMetadataReaderFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SharedMetadataReaderFactoryContextInitializer}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
class SharedMetadataReaderFactoryContextInitializerTests {

	@Test
	@SuppressWarnings("unchecked")
	void checkOrderOfInitializer() {
		SpringApplication application = new SpringApplication(TestConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		List<ApplicationContextInitializer<?>> initializers = (List<ApplicationContextInitializer<?>>) ReflectionTestUtils
				.getField(application, "initializers");
		// Simulate what would happen if an initializer was added using spring.factories
		// and happened to be loaded first
		initializers.add(0, new Initializer());
		GenericApplicationContext context = (GenericApplicationContext) application.run();
		BeanDefinition definition = context.getBeanDefinition(SharedMetadataReaderFactoryContextInitializer.BEAN_NAME);
		assertThat(definition.getAttribute("seen")).isEqualTo(true);
	}

	@Test
	void initializeWhenUsingSupplierDecorates() {
		GenericApplicationContext context = new GenericApplicationContext();
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) context.getBeanFactory();
		ConfigurationClassPostProcessor configurationAnnotationPostProcessor = mock(
				ConfigurationClassPostProcessor.class);
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.genericBeanDefinition(ConfigurationClassPostProcessor.class).getBeanDefinition();
		((AbstractBeanDefinition) beanDefinition).setInstanceSupplier(() -> configurationAnnotationPostProcessor);
		registry.registerBeanDefinition(AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME,
				beanDefinition);
		CachingMetadataReaderFactoryPostProcessor postProcessor = new CachingMetadataReaderFactoryPostProcessor(
				context);
		postProcessor.postProcessBeanDefinitionRegistry(registry);
		context.refresh();
		ConfigurationClassPostProcessor bean = context.getBean(ConfigurationClassPostProcessor.class);
		assertThat(bean).isSameAs(configurationAnnotationPostProcessor);
		ArgumentCaptor<MetadataReaderFactory> metadataReaderFactory = ArgumentCaptor
				.forClass(MetadataReaderFactory.class);
		verify(configurationAnnotationPostProcessor).setMetadataReaderFactory(metadataReaderFactory.capture());
		assertThat(metadataReaderFactory.getValue())
				.isInstanceOf(ConcurrentReferenceCachingMetadataReaderFactory.class);
	}

	static class TestConfig {

	}

	static class Initializer implements ApplicationContextInitializer<GenericApplicationContext> {

		@Override
		public void initialize(GenericApplicationContext applicationContext) {
			applicationContext.addBeanFactoryPostProcessor(new PostProcessor());
		}

	}

	static class PostProcessor implements BeanDefinitionRegistryPostProcessor {

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			for (String name : registry.getBeanDefinitionNames()) {
				BeanDefinition definition = registry.getBeanDefinition(name);
				definition.setAttribute("seen", true);
			}
		}

	}

}
