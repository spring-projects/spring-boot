/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@link ContextCustomizer} for {@link SpringBootTest}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class SpringBootTestContextCustomizer implements ContextCustomizer {

	@Override
	public void customizeContext(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedContextConfiguration) {
		SpringBootTest annotation = AnnotatedElementUtils.getMergedAnnotation(
				mergedContextConfiguration.getTestClass(), SpringBootTest.class);
		if (annotation.webEnvironment().isEmbedded()) {
			registerTestRestTemplate(context);
		}
	}

	private void registerTestRestTemplate(ConfigurableApplicationContext context) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beanFactory instanceof BeanDefinitionRegistry) {
			registerTestRestTemplate(context, (BeanDefinitionRegistry) context);
		}

	}

	private void registerTestRestTemplate(ConfigurableApplicationContext context,
			BeanDefinitionRegistry registry) {
		registry.registerBeanDefinition("testRestTemplate",
				new RootBeanDefinition(TestRestTemplateFactory.class));
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		return true;
	}

	/**
	 * {@link FactoryBean} used to create and configure a {@link TestRestTemplate}.
	 */
	public static class TestRestTemplateFactory
			implements FactoryBean<TestRestTemplate>, ApplicationContextAware {

		private TestRestTemplate object;

		@Override
		public void setApplicationContext(ApplicationContext applicationContext)
				throws BeansException {
			RestTemplateBuilder builder = getRestTemplateBuilder(applicationContext);
			TestRestTemplate template = new TestRestTemplate(builder.build());
			template.setUriTemplateHandler(
					new LocalHostUriTemplateHandler(applicationContext.getEnvironment()));
			this.object = template;
		}

		private RestTemplateBuilder getRestTemplateBuilder(
				ApplicationContext applicationContext) {
			try {
				return applicationContext.getBean(RestTemplateBuilder.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				return new RestTemplateBuilder();
			}
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

		@Override
		public Class<?> getObjectType() {
			return TestRestTemplate.class;
		}

		@Override
		public TestRestTemplate getObject() throws Exception {
			return this.object;
		}

	}

}
