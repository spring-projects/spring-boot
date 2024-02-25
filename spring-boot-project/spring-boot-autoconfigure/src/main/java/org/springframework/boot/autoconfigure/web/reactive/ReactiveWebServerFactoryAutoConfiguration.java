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

package org.springframework.boot.autoconfigure.web.reactive;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a reactive web server.
 *
 * @author Brian Clozel
 * @author Scott Frederick
 * @since 2.0.0
 */
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfiguration
@ConditionalOnClass(ReactiveHttpInputMessage.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@EnableConfigurationProperties(ServerProperties.class)
@Import({ ReactiveWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class,
		ReactiveWebServerFactoryConfiguration.EmbeddedTomcat.class,
		ReactiveWebServerFactoryConfiguration.EmbeddedJetty.class,
		ReactiveWebServerFactoryConfiguration.EmbeddedUndertow.class,
		ReactiveWebServerFactoryConfiguration.EmbeddedNetty.class })
public class ReactiveWebServerFactoryAutoConfiguration {

	/**
	 * Customizes the ReactiveWebServerFactory based on the provided server properties and
	 * SSL bundles.
	 * @param serverProperties the server properties to be used for customization
	 * @param sslBundles the SSL bundles to be used for customization (optional)
	 * @return the ReactiveWebServerFactoryCustomizer instance
	 */
	@Bean
	public ReactiveWebServerFactoryCustomizer reactiveWebServerFactoryCustomizer(ServerProperties serverProperties,
			ObjectProvider<SslBundles> sslBundles) {
		return new ReactiveWebServerFactoryCustomizer(serverProperties, sslBundles.getIfAvailable());
	}

	/**
	 * Creates a customizer for the Tomcat Reactive Web Server Factory based on the
	 * presence of the Tomcat class.
	 * @param serverProperties the server properties to be used for customizing the Tomcat
	 * Reactive Web Server Factory
	 * @return the TomcatReactiveWebServerFactoryCustomizer instance
	 */
	@Bean
	@ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat")
	public TomcatReactiveWebServerFactoryCustomizer tomcatReactiveWebServerFactoryCustomizer(
			ServerProperties serverProperties) {
		return new TomcatReactiveWebServerFactoryCustomizer(serverProperties);
	}

	/**
	 * Creates a new instance of ForwardedHeaderTransformer if no other bean of the same
	 * type is present in the application context and if the property
	 * "server.forward-headers-strategy" is set to "framework". This bean is conditionally
	 * created using the @ConditionalOnMissingBean and @ConditionalOnProperty annotations.
	 * @return The created ForwardedHeaderTransformer bean.
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(value = "server.forward-headers-strategy", havingValue = "framework")
	public ForwardedHeaderTransformer forwardedHeaderTransformer() {
		return new ForwardedHeaderTransformer();
	}

	/**
	 * Registers a {@link WebServerFactoryCustomizerBeanPostProcessor}. Registered via
	 * {@link ImportBeanDefinitionRegistrar} for early registration.
	 */
	public static class BeanPostProcessorsRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

		private ConfigurableListableBeanFactory beanFactory;

		/**
		 * Set the BeanFactory that this object runs in.
		 * <p>
		 * Invoked after population of normal bean properties but before an init callback
		 * such as InitializingBean's {@code afterPropertiesSet} or a custom init-method.
		 * Invoked after ResourceLoaderAware's {@code setResourceLoader},
		 * ApplicationEventPublisherAware's {@code setApplicationEventPublisher} and
		 * MessageSourceAware's {@code setMessageSource}.
		 * <p>
		 * This method will be invoked after any bean properties have been set and before
		 * any custom init-method or afterPropertiesSet callbacks are invoked.
		 * <p>
		 * This implementation saves the reference to the BeanFactory in a field for later
		 * use, mainly for resolving bean names specified in annotations.
		 * <p>
		 * Can be overridden in subclasses for further initialization purposes.
		 * @param beanFactory the BeanFactory object to be used by this object
		 * @throws BeansException if initialization failed
		 */
		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			if (beanFactory instanceof ConfigurableListableBeanFactory listableBeanFactory) {
				this.beanFactory = listableBeanFactory;
			}
		}

		/**
		 * Register the bean definitions for the BeanPostProcessorsRegistrar class.
		 * @param importingClassMetadata the metadata of the importing class
		 * @param registry the bean definition registry
		 */
		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			if (this.beanFactory == null) {
				return;
			}
			registerSyntheticBeanIfMissing(registry, "webServerFactoryCustomizerBeanPostProcessor",
					WebServerFactoryCustomizerBeanPostProcessor.class);
		}

		/**
		 * Registers a synthetic bean if it is missing in the given bean definition
		 * registry.
		 * @param registry the bean definition registry to register the synthetic bean
		 * with
		 * @param name the name of the synthetic bean
		 * @param beanClass the class of the synthetic bean
		 * @param <T> the type of the synthetic bean
		 */
		private <T> void registerSyntheticBeanIfMissing(BeanDefinitionRegistry registry, String name,
				Class<T> beanClass) {
			if (ObjectUtils.isEmpty(this.beanFactory.getBeanNamesForType(beanClass, true, false))) {
				RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass);
				beanDefinition.setSynthetic(true);
				registry.registerBeanDefinition(name, beanDefinition);
			}
		}

	}

}
