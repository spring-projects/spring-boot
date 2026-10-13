/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.autoconfigure.reactive;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ObjectUtils;

/**
 * {@link Configuration Configuration} for a reactive web server.
 *
 * @author Brian Clozel
 * @author Scott Frederick
 * @since 4.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ServerProperties.class)
@Import(ReactiveWebServerConfiguration.BeanPostProcessorsRegistrar.class)
public class ReactiveWebServerConfiguration {

	@Bean
	ReactiveWebServerFactoryCustomizer reactiveWebServerFactoryCustomizer(ServerProperties serverProperties,
			ObjectProvider<SslBundles> sslBundles) {
		return new ReactiveWebServerFactoryCustomizer(serverProperties, sslBundles.getIfAvailable());
	}

	/**
	 * Registers a {@link WebServerFactoryCustomizerBeanPostProcessor}. Registered via
	 * {@link ImportBeanDefinitionRegistrar} for early registration.
	 */
	static class BeanPostProcessorsRegistrar implements ImportBeanDefinitionRegistrar {

		private final @Nullable ConfigurableListableBeanFactory beanFactory;

		BeanPostProcessorsRegistrar(BeanFactory beanFactory) {
			this.beanFactory = (beanFactory instanceof ConfigurableListableBeanFactory listableBeanFactory)
					? listableBeanFactory : null;
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			if (this.beanFactory == null) {
				return;
			}
			registerSyntheticBeanIfMissing(this.beanFactory, registry, "webServerFactoryCustomizerBeanPostProcessor",
					WebServerFactoryCustomizerBeanPostProcessor.class);
		}

		private <T> void registerSyntheticBeanIfMissing(ConfigurableListableBeanFactory beanFactory,
				BeanDefinitionRegistry registry, String name, Class<T> beanClass) {
			if (ObjectUtils.isEmpty(beanFactory.getBeanNamesForType(beanClass, true, false))) {
				RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass);
				beanDefinition.setSynthetic(true);
				registry.registerBeanDefinition(name, beanDefinition);
			}
		}

	}

}
