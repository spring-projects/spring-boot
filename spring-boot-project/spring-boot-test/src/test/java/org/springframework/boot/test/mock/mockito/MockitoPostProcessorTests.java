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

package org.springframework.boot.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.mock.mockito.example.ExampleService;
import org.springframework.boot.test.mock.mockito.example.FailingExampleService;
import org.springframework.boot.test.mock.mockito.example.RealExampleService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test for {@link MockitoPostProcessor}. See also the integration tests.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Andreas Neiser
 */
class MockitoPostProcessorTests {

	@Test
	void cannotMockMultipleBeans() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		MockitoPostProcessor.register(context);
		context.register(MultipleBeans.class);
		assertThatIllegalStateException().isThrownBy(context::refresh)
				.withMessageContaining("Unable to register mock bean " + ExampleService.class.getName()
						+ " expected a single matching bean to replace " + "but found [example1, example2]");
	}

	@Test
	void cannotMockMultipleQualifiedBeans() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		MockitoPostProcessor.register(context);
		context.register(MultipleQualifiedBeans.class);
		assertThatIllegalStateException().isThrownBy(context::refresh)
				.withMessageContaining("Unable to register mock bean " + ExampleService.class.getName()
						+ " expected a single matching bean to replace " + "but found [example1, example3]");
	}

	@Test
	void canMockBeanProducedByFactoryBeanWithObjectTypeAttribute() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		MockitoPostProcessor.register(context);
		RootBeanDefinition factoryBeanDefinition = new RootBeanDefinition(TestFactoryBean.class);
		factoryBeanDefinition.setAttribute("factoryBeanObjectType", SomeInterface.class.getName());
		context.registerBeanDefinition("beanToBeMocked", factoryBeanDefinition);
		context.register(MockedFactoryBean.class);
		context.refresh();
		assertThat(Mockito.mockingDetails(context.getBean("beanToBeMocked")).isMock()).isTrue();
	}

	@Test
	void canMockPrimaryBean() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		MockitoPostProcessor.register(context);
		context.register(MockPrimaryBean.class);
		context.refresh();
		assertThat(Mockito.mockingDetails(context.getBean(MockPrimaryBean.class).mock).isMock()).isTrue();
		assertThat(Mockito.mockingDetails(context.getBean(ExampleService.class)).isMock()).isTrue();
		assertThat(Mockito.mockingDetails(context.getBean("examplePrimary", ExampleService.class)).isMock()).isTrue();
		assertThat(Mockito.mockingDetails(context.getBean("exampleQualified", ExampleService.class)).isMock())
				.isFalse();
	}

	@Test
	void canMockQualifiedBeanWithPrimaryBeanPresent() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		MockitoPostProcessor.register(context);
		context.register(MockQualifiedBean.class);
		context.refresh();
		assertThat(Mockito.mockingDetails(context.getBean(MockQualifiedBean.class).mock).isMock()).isTrue();
		assertThat(Mockito.mockingDetails(context.getBean(ExampleService.class)).isMock()).isFalse();
		assertThat(Mockito.mockingDetails(context.getBean("examplePrimary", ExampleService.class)).isMock()).isFalse();
		assertThat(Mockito.mockingDetails(context.getBean("exampleQualified", ExampleService.class)).isMock()).isTrue();
	}

	@Test
	void canSpyPrimaryBean() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		MockitoPostProcessor.register(context);
		context.register(SpyPrimaryBean.class);
		context.refresh();
		assertThat(Mockito.mockingDetails(context.getBean(SpyPrimaryBean.class).spy).isSpy()).isTrue();
		assertThat(Mockito.mockingDetails(context.getBean(ExampleService.class)).isSpy()).isTrue();
		assertThat(Mockito.mockingDetails(context.getBean("examplePrimary", ExampleService.class)).isSpy()).isTrue();
		assertThat(Mockito.mockingDetails(context.getBean("exampleQualified", ExampleService.class)).isSpy()).isFalse();
	}

	@Test
	void canSpyQualifiedBeanWithPrimaryBeanPresent() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		MockitoPostProcessor.register(context);
		context.register(SpyQualifiedBean.class);
		context.refresh();
		assertThat(Mockito.mockingDetails(context.getBean(SpyQualifiedBean.class).spy).isSpy()).isTrue();
		assertThat(Mockito.mockingDetails(context.getBean(ExampleService.class)).isSpy()).isFalse();
		assertThat(Mockito.mockingDetails(context.getBean("examplePrimary", ExampleService.class)).isSpy()).isFalse();
		assertThat(Mockito.mockingDetails(context.getBean("exampleQualified", ExampleService.class)).isSpy()).isTrue();
	}

	@Configuration(proxyBeanMethods = false)
	@MockBean(SomeInterface.class)
	static class MockedFactoryBean {

		@Bean
		public TestFactoryBean testFactoryBean() {
			return new TestFactoryBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@MockBean(ExampleService.class)
	static class MultipleBeans {

		@Bean
		public ExampleService example1() {
			return new FailingExampleService();
		}

		@Bean
		public ExampleService example2() {
			return new FailingExampleService();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleQualifiedBeans {

		@MockBean
		@Qualifier("test")
		private ExampleService mock;

		@Bean
		@Qualifier("test")
		public ExampleService example1() {
			return new FailingExampleService();
		}

		@Bean
		public ExampleService example2() {
			return new FailingExampleService();
		}

		@Bean
		@Qualifier("test")
		public ExampleService example3() {
			return new FailingExampleService();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MockPrimaryBean {

		@MockBean
		private ExampleService mock;

		@Bean
		@Qualifier("test")
		public ExampleService exampleQualified() {
			return new RealExampleService("qualified");
		}

		@Bean
		@Primary
		public ExampleService examplePrimary() {
			return new RealExampleService("primary");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MockQualifiedBean {

		@MockBean
		@Qualifier("test")
		private ExampleService mock;

		@Bean
		@Qualifier("test")
		public ExampleService exampleQualified() {
			return new RealExampleService("qualified");
		}

		@Bean
		@Primary
		public ExampleService examplePrimary() {
			return new RealExampleService("primary");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SpyPrimaryBean {

		@SpyBean
		private ExampleService spy;

		@Bean
		@Qualifier("test")
		public ExampleService exampleQualified() {
			return new RealExampleService("qualified");
		}

		@Bean
		@Primary
		public ExampleService examplePrimary() {
			return new RealExampleService("primary");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SpyQualifiedBean {

		@SpyBean
		@Qualifier("test")
		private ExampleService spy;

		@Bean
		@Qualifier("test")
		public ExampleService exampleQualified() {
			return new RealExampleService("qualified");
		}

		@Bean
		@Primary
		public ExampleService examplePrimary() {
			return new RealExampleService("primary");
		}

	}

	static class TestFactoryBean implements FactoryBean<Object> {

		@Override
		public Object getObject() {
			return new TestBean();
		}

		@Override
		public Class<?> getObjectType() {
			return null;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

	}

	interface SomeInterface {

	}

	static class TestBean implements SomeInterface {

	}

}
