/*
 * Copyright 2012-2018 the original author or authors.
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
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

/**
 * {@link ContextCustomizer} for {@link WebTestClient}.
 *
 * @author Stephane Nicoll
 */
class WebTestClientContextCustomizer implements ContextCustomizer {

	@Override
	public void customizeContext(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedConfig) {
		SpringBootTest annotation = AnnotatedElementUtils
				.getMergedAnnotation(mergedConfig.getTestClass(), SpringBootTest.class);
		if (annotation.webEnvironment().isEmbedded()) {
			registerWebTestClient(context);
		}
	}

	private void registerWebTestClient(ConfigurableApplicationContext context) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beanFactory instanceof BeanDefinitionRegistry) {
			registerWebTestClient((BeanDefinitionRegistry) beanFactory);
		}
	}

	private void registerWebTestClient(BeanDefinitionRegistry registry) {
		RootBeanDefinition definition = new RootBeanDefinition(
				WebTestClientRegistrar.class);
		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(WebTestClientRegistrar.class.getName(),
				definition);
	}

	@Override
	public boolean equals(Object obj) {
		return (obj != null && obj.getClass() == getClass());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	/**
	 * {@link BeanDefinitionRegistryPostProcessor} that runs after the
	 * {@link ConfigurationClassPostProcessor} and add a {@link WebTestClientFactory} bean
	 * definition when a {@link WebTestClient} hasn't already been registered.
	 */
	private static class WebTestClientRegistrar
			implements BeanDefinitionRegistryPostProcessor, Ordered, BeanFactoryAware {

		private BeanFactory beanFactory;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		@Override
		public int getOrder() {
			return Ordered.LOWEST_PRECEDENCE;
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
				throws BeansException {
			if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
					(ListableBeanFactory) this.beanFactory, WebTestClient.class, false,
					false).length == 0) {
				registry.registerBeanDefinition(WebTestClient.class.getName(),
						new RootBeanDefinition(WebTestClientFactory.class));
			}

		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
				throws BeansException {
		}

	}

	/**
	 * {@link FactoryBean} used to create and configure a {@link WebTestClient}.
	 */
	public static class WebTestClientFactory
			implements FactoryBean<WebTestClient>, ApplicationContextAware {

		private ApplicationContext applicationContext;

		private WebTestClient object;

		@Override
		public void setApplicationContext(ApplicationContext applicationContext)
				throws BeansException {
			this.applicationContext = applicationContext;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

		@Override
		public Class<?> getObjectType() {
			return WebTestClient.class;
		}

		@Override
		public WebTestClient getObject() throws Exception {
			if (this.object == null) {
				this.object = createWebTestClient();
			}
			return this.object;
		}

		private WebTestClient createWebTestClient() {
			boolean sslEnabled = isSslEnabled(this.applicationContext);
			String port = this.applicationContext.getEnvironment()
					.getProperty("local.server.port", "8080");
			String baseUrl = (sslEnabled ? "https" : "http") + "://localhost:" + port;
			WebTestClient.Builder builder = WebTestClient.bindToServer();
			customizeWebTestClientCodecs(builder, this.applicationContext);
			return builder.baseUrl(baseUrl).build();
		}

		private boolean isSslEnabled(ApplicationContext context) {
			try {
				AbstractReactiveWebServerFactory webServerFactory = context
						.getBean(AbstractReactiveWebServerFactory.class);
				return webServerFactory.getSsl() != null
						&& webServerFactory.getSsl().isEnabled();
			}
			catch (NoSuchBeanDefinitionException ex) {
				return false;
			}
		}

		private void customizeWebTestClientCodecs(WebTestClient.Builder clientBuilder,
				ApplicationContext context) {
			Collection<CodecCustomizer> codecCustomizers = context
					.getBeansOfType(CodecCustomizer.class).values();
			if (!CollectionUtils.isEmpty(codecCustomizers)) {
				clientBuilder.exchangeStrategies(ExchangeStrategies.builder()
						.codecs((codecs) -> codecCustomizers.forEach(
								(codecCustomizer) -> codecCustomizer.customize(codecs)))
						.build());
			}
		}

	}

}
