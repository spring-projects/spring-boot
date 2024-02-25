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

package org.springframework.boot.autoconfigure.web.servlet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletRequest;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.server.ErrorPageRegistrarBeanPostProcessor;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.WebListenerRegistrar;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for servlet web servers.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Ivan Sopov
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @since 2.0.0
 */
@AutoConfiguration(after = SslAutoConfiguration.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(ServletRequest.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(ServerProperties.class)
@Import({ ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class,
		ServletWebServerFactoryConfiguration.EmbeddedTomcat.class,
		ServletWebServerFactoryConfiguration.EmbeddedJetty.class,
		ServletWebServerFactoryConfiguration.EmbeddedUndertow.class })
public class ServletWebServerFactoryAutoConfiguration {

	/**
     * Customizes the ServletWebServerFactory based on the provided server properties, web listener registrars,
     * cookie same site suppliers, and SSL bundles.
     *
     * @param serverProperties         the server properties to customize the ServletWebServerFactory
     * @param webListenerRegistrars   the ordered list of web listener registrars to be applied
     * @param cookieSameSiteSuppliers the ordered list of cookie same site suppliers to be applied
     * @param sslBundles              the SSL bundles to be used for SSL configuration (if available)
     * @return the customized ServletWebServerFactoryCustomizer
     */
    @Bean
	public ServletWebServerFactoryCustomizer servletWebServerFactoryCustomizer(ServerProperties serverProperties,
			ObjectProvider<WebListenerRegistrar> webListenerRegistrars,
			ObjectProvider<CookieSameSiteSupplier> cookieSameSiteSuppliers, ObjectProvider<SslBundles> sslBundles) {
		return new ServletWebServerFactoryCustomizer(serverProperties, webListenerRegistrars.orderedStream().toList(),
				cookieSameSiteSuppliers.orderedStream().toList(), sslBundles.getIfAvailable());
	}

	/**
     * Creates a customizer for the TomcatServletWebServerFactory based on the presence of the Tomcat class.
     * This customizer is conditionally applied only if the Tomcat class is present in the classpath.
     * It takes the server properties as a parameter and returns a new instance of TomcatServletWebServerFactoryCustomizer.
     *
     * @param serverProperties the server properties to be used for customizing the TomcatServletWebServerFactory
     * @return a new instance of TomcatServletWebServerFactoryCustomizer
     */
    @Bean
	@ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat")
	public TomcatServletWebServerFactoryCustomizer tomcatServletWebServerFactoryCustomizer(
			ServerProperties serverProperties) {
		return new TomcatServletWebServerFactoryCustomizer(serverProperties);
	}

	/**
     * ForwardedHeaderFilterConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(value = "server.forward-headers-strategy", havingValue = "framework")
	@ConditionalOnMissingFilterBean(ForwardedHeaderFilter.class)
	static class ForwardedHeaderFilterConfiguration {

		/**
         * Creates a customizer for the ForwardedHeaderFilter that sets the relative redirects property based on the server properties.
         * 
         * @param serverProperties the server properties used to determine the value of the relative redirects property
         * @return the customizer for the ForwardedHeaderFilter
         */
        @Bean
		@ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat")
		ForwardedHeaderFilterCustomizer tomcatForwardedHeaderFilterCustomizer(ServerProperties serverProperties) {
			return (filter) -> filter.setRelativeRedirects(serverProperties.getTomcat().isUseRelativeRedirects());
		}

		/**
         * Registers the ForwardedHeaderFilter with the specified customizer.
         * 
         * @param customizerProvider the provider for the ForwardedHeaderFilterCustomizer
         * @return the FilterRegistrationBean for the ForwardedHeaderFilter
         */
        @Bean
		FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter(
				ObjectProvider<ForwardedHeaderFilterCustomizer> customizerProvider) {
			ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
			customizerProvider.ifAvailable((customizer) -> customizer.customize(filter));
			FilterRegistrationBean<ForwardedHeaderFilter> registration = new FilterRegistrationBean<>(filter);
			registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
			registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
			return registration;
		}

	}

	interface ForwardedHeaderFilterCustomizer {

		void customize(ForwardedHeaderFilter filter);

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
         * Invoked after population of normal bean properties but before an init callback such as InitializingBean's
         * {@code afterPropertiesSet} or a custom init-method. Invoked after ResourceLoaderAware's {@code setResourceLoader},
         * ApplicationEventPublisherAware's {@code setApplicationEventPublisher} and MessageSourceAware's
         * {@code setMessageSource}.
         * <p>
         * This method will be invoked after any bean properties have been set and before any custom init-method or
         * afterPropertiesSet callbacks are invoked.
         * <p>
         * This implementation saves the reference to the BeanFactory in a field for later use, mainly for
         * resolving bean names specified in annotations.
         * <p>
         * Can be overridden in subclasses for further initialization purposes.
         *
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
         * Registers the bean definitions for the BeanPostProcessorsRegistrar class.
         * 
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
			registerSyntheticBeanIfMissing(registry, "errorPageRegistrarBeanPostProcessor",
					ErrorPageRegistrarBeanPostProcessor.class);
		}

		/**
         * Registers a synthetic bean if it is missing in the given bean definition registry.
         * 
         * @param registry the bean definition registry to register the synthetic bean with
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
