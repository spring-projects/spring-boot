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

package org.springframework.boot.test.web.reactive.server;

import java.util.Collection;

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
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

/**
 * {@link ContextCustomizer} for {@link WebTestClient}.
 *
 * @author Stephane Nicoll
 */
class WebTestClientContextCustomizer implements ContextCustomizer {

	/**
	 * Customize the application context for the WebTestClient. This method is called
	 * during the initialization of the test context. It checks if the AOT (Ahead-of-Time)
	 * detection is enabled and returns if so. Otherwise, it retrieves the merged
	 * SpringBootTest annotation from the test class. If the web environment is set to
	 * embedded, it registers the WebTestClient in the context.
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
			registerWebTestClient(context);
		}
	}

	/**
	 * Registers the WebTestClient in the given ConfigurableApplicationContext.
	 * @param context the ConfigurableApplicationContext to register the WebTestClient in
	 */
	private void registerWebTestClient(ConfigurableApplicationContext context) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beanFactory instanceof BeanDefinitionRegistry registry) {
			registerWebTestClient(registry);
		}
	}

	/**
	 * Registers the WebTestClientRegistrar bean definition in the given registry. This
	 * method is used to configure the WebTestClient for testing purposes.
	 * @param registry the BeanDefinitionRegistry to register the WebTestClientRegistrar
	 * bean definition
	 * @see WebTestClientRegistrar
	 * @since 1.0
	 */
	private void registerWebTestClient(BeanDefinitionRegistry registry) {
		RootBeanDefinition definition = new RootBeanDefinition(WebTestClientRegistrar.class);
		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(WebTestClientRegistrar.class.getName(), definition);
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
	 * {@link ConfigurationClassPostProcessor} and add a {@link WebTestClientFactory} bean
	 * definition when a {@link WebTestClient} hasn't already been registered.
	 */
	static class WebTestClientRegistrar implements BeanDefinitionRegistryPostProcessor, Ordered, BeanFactoryAware {

		private BeanFactory beanFactory;

		/**
		 * Set the BeanFactory that this object runs in.
		 * <p>
		 * Invoked after population of normal bean properties but before an init callback
		 * such as InitializingBean's {@code afterPropertiesSet} or a custom init-method.
		 * Invoked after ApplicationContextAware's {@code setApplicationContext}.
		 * <p>
		 * This method allows the object instance to perform initialization based on its
		 * bean factory context, such as setting up bean references or preparing the
		 * object for use.
		 * <p>
		 * This implementation sets the specified BeanFactory as the beanFactory property
		 * of this object.
		 * @param beanFactory the BeanFactory that this object runs in
		 * @throws BeansException if initialization failed
		 */
		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		/**
		 * Returns the order value of this object.
		 *
		 * The order value indicates the relative order in which this object should be
		 * processed compared to other objects.
		 *
		 * This method returns the lowest precedence value, indicating that this object
		 * should be processed last.
		 * @return the order value of this object
		 */
		@Override
		public int getOrder() {
			return Ordered.LOWEST_PRECEDENCE;
		}

		/**
		 * Post-processes the bean definition registry.
		 * @param registry the bean definition registry
		 * @throws BeansException if an error occurs during bean processing
		 */
		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			if (AotDetector.useGeneratedArtifacts()) {
				return;
			}
			if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors((ListableBeanFactory) this.beanFactory,
					WebTestClient.class, false, false).length == 0) {
				registry.registerBeanDefinition(WebTestClient.class.getName(),
						new RootBeanDefinition(WebTestClientFactory.class));
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

	}

	/**
	 * {@link FactoryBean} used to create and configure a {@link WebTestClient}.
	 */
	public static class WebTestClientFactory implements FactoryBean<WebTestClient>, ApplicationContextAware {

		private ApplicationContext applicationContext;

		private WebTestClient object;

		private static final String SERVLET_APPLICATION_CONTEXT_CLASS = "org.springframework.web.context.WebApplicationContext";

		private static final String REACTIVE_APPLICATION_CONTEXT_CLASS = "org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext";

		/**
		 * Sets the application context for this WebTestClientFactory.
		 * @param applicationContext the application context to be set
		 * @throws BeansException if an error occurs while setting the application context
		 */
		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			this.applicationContext = applicationContext;
		}

		/**
		 * Returns a boolean value indicating whether the WebTestClientFactory is a
		 * singleton.
		 * @return true if the WebTestClientFactory is a singleton, false otherwise
		 */
		@Override
		public boolean isSingleton() {
			return true;
		}

		/**
		 * Returns the type of object that is created by this factory method.
		 * @return the type of object created by this factory method, which is
		 * {@link WebTestClient}
		 */
		@Override
		public Class<?> getObjectType() {
			return WebTestClient.class;
		}

		/**
		 * Returns the WebTestClient object.
		 * @return the WebTestClient object
		 * @throws Exception if an error occurs while creating the WebTestClient object
		 */
		@Override
		public WebTestClient getObject() throws Exception {
			if (this.object == null) {
				this.object = createWebTestClient();
			}
			return this.object;
		}

		/**
		 * Creates a new instance of WebTestClient.
		 * @return the created WebTestClient instance
		 */
		private WebTestClient createWebTestClient() {
			boolean sslEnabled = isSslEnabled(this.applicationContext);
			String port = this.applicationContext.getEnvironment().getProperty("local.server.port", "8080");
			String baseUrl = getBaseUrl(sslEnabled, port);
			WebTestClient.Builder builder = WebTestClient.bindToServer();
			customizeWebTestClientBuilder(builder, this.applicationContext);
			customizeWebTestClientCodecs(builder, this.applicationContext);
			return builder.baseUrl(baseUrl).build();
		}

		/**
		 * Returns the base URL for the WebTestClient.
		 * @param sslEnabled a boolean indicating whether SSL is enabled
		 * @param port the port number
		 * @return the base URL as a String
		 */
		private String getBaseUrl(boolean sslEnabled, String port) {
			String basePath = deduceBasePath();
			String pathSegment = (StringUtils.hasText(basePath)) ? basePath : "";
			return (sslEnabled ? "https" : "http") + "://localhost:" + port + pathSegment;
		}

		/**
		 * Deduces the base path for the web application based on the application context.
		 * @return the base path for the web application, or null if it cannot be deduced
		 */
		private String deduceBasePath() {
			WebApplicationType webApplicationType = deduceFromApplicationContext(this.applicationContext.getClass());
			if (webApplicationType == WebApplicationType.REACTIVE) {
				return this.applicationContext.getEnvironment().getProperty("spring.webflux.base-path");
			}
			else if (webApplicationType == WebApplicationType.SERVLET) {
				return ((WebApplicationContext) this.applicationContext).getServletContext().getContextPath();
			}
			return null;
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
		 * Checks if SSL is enabled for the given application context.
		 * @param context the application context
		 * @return true if SSL is enabled, false otherwise
		 */
		private boolean isSslEnabled(ApplicationContext context) {
			try {
				AbstractReactiveWebServerFactory webServerFactory = context
					.getBean(AbstractReactiveWebServerFactory.class);
				return webServerFactory.getSsl() != null && webServerFactory.getSsl().isEnabled();
			}
			catch (NoSuchBeanDefinitionException ex) {
				return false;
			}
		}

		/**
		 * Customizes the WebTestClient.Builder by applying customizers from the
		 * ApplicationContext.
		 * @param clientBuilder the WebTestClient.Builder to be customized
		 * @param context the ApplicationContext containing the customizers
		 */
		private void customizeWebTestClientBuilder(WebTestClient.Builder clientBuilder, ApplicationContext context) {
			for (WebTestClientBuilderCustomizer customizer : context
				.getBeansOfType(WebTestClientBuilderCustomizer.class)
				.values()) {
				customizer.customize(clientBuilder);
			}
		}

		/**
		 * Customizes the codecs used by the WebTestClient.
		 * @param clientBuilder the WebTestClient.Builder instance
		 * @param context the ApplicationContext instance
		 */
		private void customizeWebTestClientCodecs(WebTestClient.Builder clientBuilder, ApplicationContext context) {
			Collection<CodecCustomizer> codecCustomizers = context.getBeansOfType(CodecCustomizer.class).values();
			if (!CollectionUtils.isEmpty(codecCustomizers)) {
				clientBuilder.exchangeStrategies(ExchangeStrategies.builder()
					.codecs((codecs) -> codecCustomizers
						.forEach((codecCustomizer) -> codecCustomizer.customize(codecs)))
					.build());
			}
		}

	}

}
