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

package org.springframework.boot.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.mock.mockito.example.ExampleGenericService;
import org.springframework.boot.test.mock.mockito.example.SimpleExampleStringGenericService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test {@link SpyBean @SpyBean} on a test class field can be used to replace an existing
 * bean with generics that's produced by a factory bean.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(SpringExtension.class)
class SpyBeanOnTestFieldForExistingGenericBeanProducedByFactoryBeanIntegrationTests {

	// gh-40234

	@SpyBean(name = "exampleService")
	private ExampleGenericService<String> exampleService;

	@Test
	void testSpying() {
		assertThat(Mockito.mockingDetails(this.exampleService).isSpy()).isTrue();
		assertThat(Mockito.mockingDetails(this.exampleService).getMockCreationSettings().getSpiedInstance())
			.isInstanceOf(SimpleExampleStringGenericService.class);
	}

	@Configuration(proxyBeanMethods = false)
	@Import(FactoryBeanRegistrar.class)
	static class SpyBeanOnTestFieldForExistingBeanConfig {

	}

	static class FactoryBeanRegistrar implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			RootBeanDefinition definition = new RootBeanDefinition(ExampleGenericServiceFactoryBean.class);
			definition.setTargetType(ResolvableType.forClassWithGenerics(ExampleGenericServiceFactoryBean.class, null,
					ExampleGenericService.class));
			registry.registerBeanDefinition("exampleService", definition);
		}

	}

	static class ExampleGenericServiceFactoryBean<T, U extends ExampleGenericService<T>> implements FactoryBean<U> {

		@SuppressWarnings("unchecked")
		@Override
		public U getObject() throws Exception {
			return (U) new SimpleExampleStringGenericService();
		}

		@Override
		@SuppressWarnings("rawtypes")
		public Class<ExampleGenericService> getObjectType() {
			return ExampleGenericService.class;
		}

	}

}
