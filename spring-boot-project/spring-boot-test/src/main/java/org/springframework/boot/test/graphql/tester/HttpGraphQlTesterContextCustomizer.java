/*
 * Copyright 2020-2023 the original author or authors.
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

package org.springframework.boot.test.graphql.tester;

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
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link ContextCustomizer} for {@link HttpGraphQlTester}.
 *
 * @author Brian Clozel
 */
class HttpGraphQlTesterContextCustomizer implements ContextCustomizer {

	/**
	 * Customizes the application context for the HttpGraphQlTester.
	 * @param context the configurable application context
	 * @param mergedConfig the merged context configuration
	 */
	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		if (AotDetector.useGeneratedArtifacts()) {
			return;
		}
		SpringBootTest springBootTest = TestContextAnnotationUtils.findMergedAnnotation(mergedConfig.getTestClass(),
				SpringBootTest.class);
		if (springBootTest.webEnvironment().isEmbedded()) {
			registerHttpGraphQlTester(context);
		}
	}

	/**
	 * Registers the HttpGraphQlTester in the given ConfigurableApplicationContext.
	 * @param context the ConfigurableApplicationContext to register the HttpGraphQlTester
	 * in
	 */
	private void registerHttpGraphQlTester(ConfigurableApplicationContext context) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beanFactory instanceof BeanDefinitionRegistry) {
			registerHttpGraphQlTester((BeanDefinitionRegistry) beanFactory);
		}
	}

	/**
	 * Registers the HttpGraphQlTesterRegistrar bean definition in the given registry.
	 * This method is responsible for creating and configuring the
	 * HttpGraphQlTesterRegistrar bean, and registering it in the provided
	 * BeanDefinitionRegistry.
	 * @param registry the BeanDefinitionRegistry in which to register the
	 * HttpGraphQlTesterRegistrar bean definition
	 */
	private void registerHttpGraphQlTester(BeanDefinitionRegistry registry) {
		RootBeanDefinition definition = new RootBeanDefinition(HttpGraphQlTesterRegistrar.class);
		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(HttpGraphQlTesterRegistrar.class.getName(), definition);
	}

	/**
	 * Compares this object with the specified object for equality.
	 * @param obj the object to compare with
	 * @return {@code true} if the specified object is of the same class as this object,
	 * {@code false} otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj != null) && (obj.getClass() == getClass());
	}

	/**
	 * Returns a hash code value for the object. This method overrides the default
	 * implementation of the hashCode() method.
	 * @return the hash code value for this object
	 */
	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	/**
	 * HttpGraphQlTesterRegistrar class.
	 */
	static class HttpGraphQlTesterRegistrar implements BeanDefinitionRegistryPostProcessor, Ordered, BeanFactoryAware {

		private BeanFactory beanFactory;

		/**
		 * Sets the bean factory for this HttpGraphQlTesterRegistrar.
		 * @param beanFactory the bean factory to be set
		 * @throws BeansException if an error occurs while setting the bean factory
		 */
		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		/**
		 * This method is called to post-process the bean definition registry. It checks
		 * if the AotDetector is using generated artifacts. If so, it returns. Otherwise,
		 * it checks if there are any bean names for the HttpGraphQlTester class in the
		 * bean factory. If there are none, it registers a new bean definition for the
		 * HttpGraphQlTester class using the HttpGraphQlTesterFactory.
		 * @param registry the bean definition registry to post-process
		 * @throws BeansException if an error occurs during the post-processing
		 */
		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			if (AotDetector.useGeneratedArtifacts()) {
				return;
			}
			if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors((ListableBeanFactory) this.beanFactory,
					HttpGraphQlTester.class, false, false).length == 0) {
				registry.registerBeanDefinition(HttpGraphQlTester.class.getName(),
						new RootBeanDefinition(HttpGraphQlTesterFactory.class));
			}
		}

		/**
		 * Post-process the given bean factory.
		 * @param beanFactory the bean factory to post-process
		 * @throws BeansException if an error occurs during post-processing
		 */
		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		}

		/**
		 * Returns the order of this HttpGraphQlTesterRegistrar bean in the
		 * ApplicationContext. The order is set to be one less than the lowest precedence
		 * value, ensuring that this bean is registered last.
		 * @return the order of this HttpGraphQlTesterRegistrar bean
		 */
		@Override
		public int getOrder() {
			return Ordered.LOWEST_PRECEDENCE - 1;
		}

	}

	/**
	 * HttpGraphQlTesterFactory class.
	 */
	public static class HttpGraphQlTesterFactory implements FactoryBean<HttpGraphQlTester>, ApplicationContextAware {

		private static final String SERVLET_APPLICATION_CONTEXT_CLASS = "org.springframework.web.context.WebApplicationContext";

		private static final String REACTIVE_APPLICATION_CONTEXT_CLASS = "org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext";

		private ApplicationContext applicationContext;

		private HttpGraphQlTester object;

		/**
		 * Sets the application context for this HttpGraphQlTesterFactory.
		 * @param applicationContext the application context to be set
		 * @throws BeansException if an error occurs while setting the application context
		 */
		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			this.applicationContext = applicationContext;
		}

		/**
		 * Returns a boolean value indicating whether the HttpGraphQlTesterFactory is a
		 * singleton.
		 * @return true if the HttpGraphQlTesterFactory is a singleton, false otherwise.
		 */
		@Override
		public boolean isSingleton() {
			return true;
		}

		/**
		 * Returns the type of object that is created by this factory method.
		 * @return the type of object created by this factory method, which is
		 * HttpGraphQlTester class.
		 */
		@Override
		public Class<?> getObjectType() {
			return HttpGraphQlTester.class;
		}

		/**
		 * Retrieves the HttpGraphQlTester object.
		 * @return The HttpGraphQlTester object.
		 * @throws Exception if an error occurs while creating the HttpGraphQlTester
		 * object.
		 */
		@Override
		public HttpGraphQlTester getObject() throws Exception {
			if (this.object == null) {
				this.object = createGraphQlTester();
			}
			return this.object;
		}

		/**
		 * Creates a new instance of HttpGraphQlTester.
		 * @return the newly created HttpGraphQlTester instance
		 */
		private HttpGraphQlTester createGraphQlTester() {
			WebTestClient webTestClient = this.applicationContext.getBean(WebTestClient.class);
			boolean sslEnabled = isSslEnabled(this.applicationContext);
			String port = this.applicationContext.getEnvironment().getProperty("local.server.port", "8080");
			WebTestClient mutatedWebClient = webTestClient.mutate().baseUrl(getBaseUrl(sslEnabled, port)).build();
			return HttpGraphQlTester.create(mutatedWebClient);
		}

		/**
		 * Returns the base URL for the HTTP server.
		 * @param sslEnabled a boolean indicating whether SSL is enabled or not
		 * @param port the port number for the server
		 * @return the base URL for the server
		 */
		private String getBaseUrl(boolean sslEnabled, String port) {
			String basePath = deduceBasePath();
			return (sslEnabled ? "https" : "http") + "://localhost:" + port + basePath;
		}

		/**
		 * Deduces the base path for the GraphQL endpoint by combining the server base
		 * path and the configured GraphQL path.
		 * @return the deduced base path for the GraphQL endpoint
		 */
		private String deduceBasePath() {
			return deduceServerBasePath() + findConfiguredGraphQlPath();
		}

		/**
		 * Returns the configured GraphQL path.
		 * @return the configured GraphQL path, or "/graphql" if not configured
		 */
		private String findConfiguredGraphQlPath() {
			String configuredPath = this.applicationContext.getEnvironment().getProperty("spring.graphql.path");
			return StringUtils.hasText(configuredPath) ? configuredPath : "/graphql";
		}

		/**
		 * Deduces the server base path based on the web application type.
		 * @return The server base path.
		 */
		private String deduceServerBasePath() {
			String serverBasePath = "";
			WebApplicationType webApplicationType = deduceFromApplicationContext(this.applicationContext.getClass());
			if (webApplicationType == WebApplicationType.REACTIVE) {
				serverBasePath = this.applicationContext.getEnvironment().getProperty("spring.webflux.base-path");

			}
			else if (webApplicationType == WebApplicationType.SERVLET) {
				serverBasePath = ((WebApplicationContext) this.applicationContext).getServletContext().getContextPath();
			}
			return (serverBasePath != null) ? serverBasePath : "";
		}

		/**
		 * Deduces the web application type based on the given application context class.
		 * @param applicationContextClass the class of the application context
		 * @return the deduced web application type
		 */
		static WebApplicationType deduceFromApplicationContext(Class<?> applicationContextClass) {
			if (isAssignable(SERVLET_APPLICATION_CONTEXT_CLASS, applicationContextClass)) {
				return WebApplicationType.SERVLET;
			}
			if (isAssignable(REACTIVE_APPLICATION_CONTEXT_CLASS, applicationContextClass)) {
				return WebApplicationType.REACTIVE;
			}
			return WebApplicationType.NONE;
		}

		/**
		 * Checks if the given target class name is assignable from the specified type.
		 * @param target the target class name to check
		 * @param type the type to check against
		 * @return {@code true} if the target class is assignable from the specified type,
		 * {@code false} otherwise
		 */
		private static boolean isAssignable(String target, Class<?> type) {
			try {
				return ClassUtils.resolveClassName(target, null).isAssignableFrom(type);
			}
			catch (Throwable ex) {
				return false;
			}
		}

		/**
		 * Checks if SSL is enabled for the web server factory in the given application
		 * context.
		 * @param context the application context
		 * @return true if SSL is enabled, false otherwise
		 */
		private boolean isSslEnabled(ApplicationContext context) {
			try {
				AbstractConfigurableWebServerFactory webServerFactory = context
					.getBean(AbstractConfigurableWebServerFactory.class);
				return webServerFactory.getSsl() != null && webServerFactory.getSsl().isEnabled();
			}
			catch (NoSuchBeanDefinitionException ex) {
				return false;
			}
		}

	}

}
