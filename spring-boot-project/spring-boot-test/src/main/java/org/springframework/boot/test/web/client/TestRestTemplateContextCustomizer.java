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

package org.springframework.boot.test.web.client;

import org.springframework.aot.AotDetector;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate.HttpClientOption;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizer} for {@link TestRestTemplate}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class TestRestTemplateContextCustomizer implements ContextCustomizer {

	/**
	 * Customizes the application context for the test by registering a TestRestTemplate
	 * if the test is annotated with @SpringBootTest and has an embedded web environment.
	 * @param context the configurable application context
	 * @param mergedContextConfiguration the merged context configuration
	 */
	@Override
	public void customizeContext(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedContextConfiguration) {
		if (AotDetector.useGeneratedArtifacts()) {
			return;
		}
		SpringBootTest springBootTest = TestContextAnnotationUtils
			.findMergedAnnotation(mergedContextConfiguration.getTestClass(), SpringBootTest.class);
		if (springBootTest.webEnvironment().isEmbedded()) {
			registerTestRestTemplate(context);
		}
	}

	/**
	 * Registers a TestRestTemplate in the given ConfigurableApplicationContext.
	 * @param context the ConfigurableApplicationContext to register the TestRestTemplate
	 * in
	 */
	private void registerTestRestTemplate(ConfigurableApplicationContext context) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beanFactory instanceof BeanDefinitionRegistry registry) {
			registerTestRestTemplate(registry);
		}
	}

	/**
	 * Registers the TestRestTemplateRegistrar bean definition in the given registry. This
	 * method is used to configure the TestRestTemplateRegistrar bean, which is
	 * responsible for registering the TestRestTemplate bean in the application context.
	 * @param registry the BeanDefinitionRegistry in which the TestRestTemplateRegistrar
	 * bean definition will be registered
	 */
	private void registerTestRestTemplate(BeanDefinitionRegistry registry) {
		RootBeanDefinition definition = new RootBeanDefinition(TestRestTemplateRegistrar.class);
		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(TestRestTemplateRegistrar.class.getName(), definition);
	}

	/**
	 * Compares this object with the specified object for equality.
	 * @param obj the object to compare with
	 * @return {@code true} if the specified object is equal to this object, {@code false}
	 * otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj != null) && (obj.getClass() == getClass());
	}

	/**
	 * Returns a hash code value for the object. This method overrides the hashCode()
	 * method in the Object class.
	 * @return the hash code value for the object
	 */
	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	/**
	 * {@link BeanDefinitionRegistryPostProcessor} that runs after the
	 * {@link ConfigurationClassPostProcessor} and add a {@link TestRestTemplateFactory}
	 * bean definition when a {@link TestRestTemplate} hasn't already been registered.
	 */
	static class TestRestTemplateRegistrar implements BeanDefinitionRegistryPostProcessor, Ordered, BeanFactoryAware {

		private BeanFactory beanFactory;

		/**
		 * Sets the bean factory for this TestRestTemplateRegistrar.
		 * @param beanFactory the bean factory to set
		 * @throws BeansException if an error occurs while setting the bean factory
		 */
		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		/**
		 * Returns the order of this TestRestTemplateRegistrar. The order is set to
		 * LOWEST_PRECEDENCE.
		 * @return the order of this TestRestTemplateRegistrar
		 */
		@Override
		public int getOrder() {
			return Ordered.LOWEST_PRECEDENCE;
		}

		/**
		 * This method is used to post-process the bean definition registry. It checks if
		 * the AotDetector is using generated artifacts and returns if true. If there are
		 * no bean names for the TestRestTemplate class in the bean factory, it registers
		 * a new bean definition for the TestRestTemplate class using the
		 * TestRestTemplateFactory.
		 * @param registry the bean definition registry to be processed
		 * @throws BeansException if an error occurs during the bean processing
		 */
		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			if (AotDetector.useGeneratedArtifacts()) {
				return;
			}
			if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors((ListableBeanFactory) this.beanFactory,
					TestRestTemplate.class, false, false).length == 0) {
				registry.registerBeanDefinition(TestRestTemplate.class.getName(),
						new RootBeanDefinition(TestRestTemplateFactory.class));
			}

		}

		/**
		 * Post-process the given bean factory.
		 * @param beanFactory the bean factory to post-process
		 * @throws BeansException if any error occurs during post-processing
		 */
		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		}

	}

	/**
	 * {@link FactoryBean} used to create and configure a {@link TestRestTemplate}.
	 */
	public static class TestRestTemplateFactory implements FactoryBean<TestRestTemplate>, ApplicationContextAware {

		private static final HttpClientOption[] DEFAULT_OPTIONS = {};

		private static final HttpClientOption[] SSL_OPTIONS = { HttpClientOption.SSL };

		private TestRestTemplate template;

		/**
		 * Sets the application context and initializes the TestRestTemplate.
		 * @param applicationContext the application context to set
		 * @throws BeansException if an error occurs while setting the application context
		 */
		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			RestTemplateBuilder builder = getRestTemplateBuilder(applicationContext);
			boolean sslEnabled = isSslEnabled(applicationContext);
			TestRestTemplate template = new TestRestTemplate(builder, null, null,
					sslEnabled ? SSL_OPTIONS : DEFAULT_OPTIONS);
			LocalHostUriTemplateHandler handler = new LocalHostUriTemplateHandler(applicationContext.getEnvironment(),
					sslEnabled ? "https" : "http");
			template.setUriTemplateHandler(handler);
			this.template = template;
		}

		/**
		 * Checks if SSL is enabled for the given application context.
		 * @param context the application context
		 * @return true if SSL is enabled, false otherwise
		 */
		private boolean isSslEnabled(ApplicationContext context) {
			try {
				AbstractServletWebServerFactory webServerFactory = context
					.getBean(AbstractServletWebServerFactory.class);
				return webServerFactory.getSsl() != null && webServerFactory.getSsl().isEnabled();
			}
			catch (NoSuchBeanDefinitionException ex) {
				return false;
			}
		}

		/**
		 * Returns a RestTemplateBuilder object based on the provided ApplicationContext.
		 * If a RestTemplateBuilder bean is found in the ApplicationContext, it is
		 * returned. Otherwise, a new RestTemplateBuilder object is created and returned.
		 * @param applicationContext the ApplicationContext to retrieve the
		 * RestTemplateBuilder bean from
		 * @return a RestTemplateBuilder object
		 */
		private RestTemplateBuilder getRestTemplateBuilder(ApplicationContext applicationContext) {
			try {
				return applicationContext.getBean(RestTemplateBuilder.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				return new RestTemplateBuilder();
			}
		}

		/**
		 * Returns a boolean value indicating whether the TestRestTemplateFactory is a
		 * singleton.
		 * @return true if the TestRestTemplateFactory is a singleton, false otherwise
		 */
		@Override
		public boolean isSingleton() {
			return true;
		}

		/**
		 * Returns the type of object that is created by this factory method.
		 * @return the type of object created by this factory method, which is
		 * TestRestTemplate
		 */
		@Override
		public Class<?> getObjectType() {
			return TestRestTemplate.class;
		}

		/**
		 * Returns the TestRestTemplate object.
		 * @return the TestRestTemplate object
		 * @throws Exception if an error occurs while retrieving the TestRestTemplate
		 * object
		 */
		@Override
		public TestRestTemplate getObject() throws Exception {
			return this.template;
		}

	}

}
