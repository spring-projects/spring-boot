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

	private void registerHttpGraphQlTester(ConfigurableApplicationContext context) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beanFactory instanceof BeanDefinitionRegistry) {
			registerHttpGraphQlTester((BeanDefinitionRegistry) beanFactory);
		}
	}

	private void registerHttpGraphQlTester(BeanDefinitionRegistry registry) {
		RootBeanDefinition definition = new RootBeanDefinition(HttpGraphQlTesterRegistrar.class);
		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(HttpGraphQlTesterRegistrar.class.getName(), definition);
	}

	@Override
	public boolean equals(Object obj) {
		return (obj != null) && (obj.getClass() == getClass());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	static class HttpGraphQlTesterRegistrar implements BeanDefinitionRegistryPostProcessor, Ordered, BeanFactoryAware {

		private BeanFactory beanFactory;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

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

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		}

		@Override
		public int getOrder() {
			return Ordered.LOWEST_PRECEDENCE - 1;
		}

	}

	public static class HttpGraphQlTesterFactory implements FactoryBean<HttpGraphQlTester>, ApplicationContextAware {

		private static final String SERVLET_APPLICATION_CONTEXT_CLASS = "org.springframework.web.context.WebApplicationContext";

		private static final String REACTIVE_APPLICATION_CONTEXT_CLASS = "org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext";

		private ApplicationContext applicationContext;

		private HttpGraphQlTester object;

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			this.applicationContext = applicationContext;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

		@Override
		public Class<?> getObjectType() {
			return HttpGraphQlTester.class;
		}

		@Override
		public HttpGraphQlTester getObject() throws Exception {
			if (this.object == null) {
				this.object = createGraphQlTester();
			}
			return this.object;
		}

		private HttpGraphQlTester createGraphQlTester() {
			WebTestClient webTestClient = this.applicationContext.getBean(WebTestClient.class);
			boolean sslEnabled = isSslEnabled(this.applicationContext);
			String port = this.applicationContext.getEnvironment().getProperty("local.server.port", "8080");
			WebTestClient mutatedWebClient = webTestClient.mutate().baseUrl(getBaseUrl(sslEnabled, port)).build();
			return HttpGraphQlTester.create(mutatedWebClient);
		}

		private String getBaseUrl(boolean sslEnabled, String port) {
			String basePath = deduceBasePath();
			return (sslEnabled ? "https" : "http") + "://localhost:" + port + basePath;
		}

		private String deduceBasePath() {
			return deduceServerBasePath() + findConfiguredGraphQlPath();
		}

		private String findConfiguredGraphQlPath() {
			String configuredPath = this.applicationContext.getEnvironment().getProperty("spring.graphql.path");
			return StringUtils.hasText(configuredPath) ? configuredPath : "/graphql";
		}

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

		static WebApplicationType deduceFromApplicationContext(Class<?> applicationContextClass) {
			if (isAssignable(SERVLET_APPLICATION_CONTEXT_CLASS, applicationContextClass)) {
				return WebApplicationType.SERVLET;
			}
			if (isAssignable(REACTIVE_APPLICATION_CONTEXT_CLASS, applicationContextClass)) {
				return WebApplicationType.REACTIVE;
			}
			return WebApplicationType.NONE;
		}

		private static boolean isAssignable(String target, Class<?> type) {
			try {
				return ClassUtils.resolveClassName(target, null).isAssignableFrom(type);
			}
			catch (Throwable ex) {
				return false;
			}
		}

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
