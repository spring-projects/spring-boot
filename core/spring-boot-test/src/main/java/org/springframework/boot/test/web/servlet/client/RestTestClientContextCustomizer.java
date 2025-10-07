/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.test.web.servlet.client;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.AotDetector;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.http.server.BaseUrl;
import org.springframework.boot.test.http.server.BaseUrlProviders;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.Assert;

/**
 * {@link ContextCustomizer} for {@link RestTestClient}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class RestTestClientContextCustomizer implements ContextCustomizer {

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		if (AotDetector.useGeneratedArtifacts()) {
			return;
		}
		SpringBootTest springBootTest = TestContextAnnotationUtils.findMergedAnnotation(mergedConfig.getTestClass(),
				SpringBootTest.class);
		Assert.state(springBootTest != null, "'springBootTest' must not be null");
		if (springBootTest.webEnvironment().isEmbedded()) {
			registerRestTestClient(context);
		}
	}

	private void registerRestTestClient(ConfigurableApplicationContext context) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beanFactory instanceof BeanDefinitionRegistry registry) {
			registerRestTestClient(registry);
		}
	}

	private void registerRestTestClient(BeanDefinitionRegistry registry) {
		RootBeanDefinition definition = new RootBeanDefinition(RestTestClientRegistrar.class);
		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(RestTestClientRegistrar.class.getName(), definition);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return (obj != null) && (obj.getClass() == getClass());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	/**
	 * {@link BeanDefinitionRegistryPostProcessor} that runs after the
	 * {@link ConfigurationClassPostProcessor} and add a {@link RestTestClientFactory}
	 * bean definition when a {@link RestTestClient} hasn't already been registered.
	 */
	static class RestTestClientRegistrar implements BeanDefinitionRegistryPostProcessor, Ordered, BeanFactoryAware {

		private @Nullable BeanFactory beanFactory;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		@Override
		public int getOrder() {
			return Ordered.LOWEST_PRECEDENCE;
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			if (this.beanFactory == null || AotDetector.useGeneratedArtifacts()) {
				return;
			}
			if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors((ListableBeanFactory) this.beanFactory,
					RestTestClient.class, false, false).length == 0) {
				registry.registerBeanDefinition(RestTestClient.class.getName(),
						new RootBeanDefinition(RestTestClientFactory.class));
			}

		}

	}

	/**
	 * {@link FactoryBean} used to create and configure a {@link RestTestClient}.
	 */
	public static class RestTestClientFactory implements FactoryBean<RestTestClient>, ApplicationContextAware {

		private @Nullable ApplicationContext applicationContext;

		private @Nullable RestTestClient object;

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			this.applicationContext = applicationContext;
		}

		@Override
		public Class<?> getObjectType() {
			return RestTestClient.class;
		}

		@Override
		public RestTestClient getObject() {
			if (this.object == null) {
				this.object = createRestTestClient();
			}
			return this.object;
		}

		private RestTestClient createRestTestClient() {
			Assert.state(this.applicationContext != null, "ApplicationContext not injected");
			RestTestClient.Builder<?> builder = RestTestClient.bindToServer();
			customizeRestTestClientBuilder(builder);
			BaseUrl baseUrl = new BaseUrlProviders(this.applicationContext).getBaseUrl();
			return builder.uriBuilderFactory(BaseUrlUriBuilderFactory.get(baseUrl)).build();
		}

		private void customizeRestTestClientBuilder(RestTestClient.Builder<?> clientBuilder) {
			Assert.state(this.applicationContext != null, "ApplicationContext not injected");
			getRestTestClientBuilderCustomizers(this.applicationContext)
				.forEach((customizer) -> customizer.customize(clientBuilder));
		}

		private List<RestTestClientBuilderCustomizer> getRestTestClientBuilderCustomizers(ApplicationContext context) {
			List<RestTestClientBuilderCustomizer> customizers = new ArrayList<>();
			customizers.addAll(SpringFactoriesLoader.forDefaultResourceLocation(context.getClassLoader())
				.load(RestTestClientBuilderCustomizer.class, ArgumentResolver.of(ApplicationContext.class, context)));
			customizers.addAll(context.getBeansOfType(RestTestClientBuilderCustomizer.class).values());
			return customizers;
		}

	}

}
