/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive;

import java.time.Duration;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidatorAdapter;
import org.springframework.boot.autoconfigure.web.ConditionalOnEnabledResourceChain;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.format.WebConversionService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.boot.web.reactive.filter.OrderedHiddenHttpMethodFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Validator;
import org.springframework.web.filter.reactive.HiddenHttpMethodFilter;
import org.springframework.web.reactive.config.DelegatingWebFluxConfiguration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceHandlerRegistration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurationSupport;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.view.ViewResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link EnableWebFlux WebFlux}.
 *
 * @author Brian Clozel
 * @author Rob Winch
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Artsiom Yudovin
 * @since 2.0.0
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(WebFluxConfigurer.class)
@ConditionalOnMissingBean({ WebFluxConfigurationSupport.class })
@AutoConfigureAfter({ ReactiveWebServerFactoryAutoConfiguration.class,
		CodecsAutoConfiguration.class, ValidationAutoConfiguration.class })
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)
public class WebFluxAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(HiddenHttpMethodFilter.class)
	@ConditionalOnProperty(prefix = "spring.webflux.hiddenmethod.filter", name = "enabled", matchIfMissing = true)
	public OrderedHiddenHttpMethodFilter hiddenHttpMethodFilter() {
		return new OrderedHiddenHttpMethodFilter();
	}

	@Configuration
	@EnableConfigurationProperties({ ResourceProperties.class, WebFluxProperties.class })
	@Import({ EnableWebFluxConfiguration.class })
	public static class WebFluxConfig implements WebFluxConfigurer {

		private static final Log logger = LogFactory.getLog(WebFluxConfig.class);

		private final ResourceProperties resourceProperties;

		private final WebFluxProperties webFluxProperties;

		private final ListableBeanFactory beanFactory;

		private final ObjectProvider<HandlerMethodArgumentResolver> argumentResolvers;

		private final ObjectProvider<CodecCustomizer> codecCustomizers;

		private final ResourceHandlerRegistrationCustomizer resourceHandlerRegistrationCustomizer;

		private final ObjectProvider<ViewResolver> viewResolvers;

		public WebFluxConfig(ResourceProperties resourceProperties,
				WebFluxProperties webFluxProperties, ListableBeanFactory beanFactory,
				ObjectProvider<HandlerMethodArgumentResolver> resolvers,
				ObjectProvider<CodecCustomizer> codecCustomizers,
				ObjectProvider<ResourceHandlerRegistrationCustomizer> resourceHandlerRegistrationCustomizer,
				ObjectProvider<ViewResolver> viewResolvers) {
			this.resourceProperties = resourceProperties;
			this.webFluxProperties = webFluxProperties;
			this.beanFactory = beanFactory;
			this.argumentResolvers = resolvers;
			this.codecCustomizers = codecCustomizers;
			this.resourceHandlerRegistrationCustomizer = resourceHandlerRegistrationCustomizer
					.getIfAvailable();
			this.viewResolvers = viewResolvers;
		}

		@Override
		public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
			this.argumentResolvers.orderedStream().forEach(configurer::addCustomResolver);
		}

		@Override
		public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
			this.codecCustomizers.orderedStream()
					.forEach((customizer) -> customizer.customize(configurer));
		}

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			if (!this.resourceProperties.isAddMappings()) {
				logger.debug("Default resource handling disabled");
				return;
			}
			if (!registry.hasMappingForPattern("/webjars/**")) {
				ResourceHandlerRegistration registration = registry
						.addResourceHandler("/webjars/**")
						.addResourceLocations("classpath:/META-INF/resources/webjars/");
				configureResourceCaching(registration);
				customizeResourceHandlerRegistration(registration);
			}
			String staticPathPattern = this.webFluxProperties.getStaticPathPattern();
			if (!registry.hasMappingForPattern(staticPathPattern)) {
				ResourceHandlerRegistration registration = registry
						.addResourceHandler(staticPathPattern).addResourceLocations(
								this.resourceProperties.getStaticLocations());
				configureResourceCaching(registration);
				customizeResourceHandlerRegistration(registration);
			}
		}

		private void configureResourceCaching(ResourceHandlerRegistration registration) {
			Duration cachePeriod = this.resourceProperties.getCache().getPeriod();
			ResourceProperties.Cache.Cachecontrol cacheControl = this.resourceProperties
					.getCache().getCachecontrol();
			if (cachePeriod != null && cacheControl.getMaxAge() == null) {
				cacheControl.setMaxAge(cachePeriod);
			}
			registration.setCacheControl(cacheControl.toHttpCacheControl());
		}

		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			this.viewResolvers.orderedStream().forEach(registry::viewResolver);
		}

		@Override
		public void addFormatters(FormatterRegistry registry) {
			for (Converter<?, ?> converter : getBeansOfType(Converter.class)) {
				registry.addConverter(converter);
			}
			for (GenericConverter converter : getBeansOfType(GenericConverter.class)) {
				registry.addConverter(converter);
			}
			for (Formatter<?> formatter : getBeansOfType(Formatter.class)) {
				registry.addFormatter(formatter);
			}
		}

		private <T> Collection<T> getBeansOfType(Class<T> type) {
			return this.beanFactory.getBeansOfType(type).values();
		}

		private void customizeResourceHandlerRegistration(
				ResourceHandlerRegistration registration) {
			if (this.resourceHandlerRegistrationCustomizer != null) {
				this.resourceHandlerRegistrationCustomizer.customize(registration);
			}

		}

	}

	/**
	 * Configuration equivalent to {@code @EnableWebFlux}.
	 */
	@Configuration
	public static class EnableWebFluxConfiguration
			extends DelegatingWebFluxConfiguration {

		private final WebFluxProperties webFluxProperties;

		private final WebFluxRegistrations webFluxRegistrations;

		public EnableWebFluxConfiguration(WebFluxProperties webFluxProperties,
				ObjectProvider<WebFluxRegistrations> webFluxRegistrations) {
			this.webFluxProperties = webFluxProperties;
			this.webFluxRegistrations = webFluxRegistrations.getIfUnique();
		}

		@Bean
		@Override
		public FormattingConversionService webFluxConversionService() {
			WebConversionService conversionService = new WebConversionService(
					this.webFluxProperties.getDateFormat());
			addFormatters(conversionService);
			return conversionService;
		}

		@Bean
		@Override
		public Validator webFluxValidator() {
			if (!ClassUtils.isPresent("javax.validation.Validator",
					getClass().getClassLoader())) {
				return super.webFluxValidator();
			}
			return ValidatorAdapter.get(getApplicationContext(), getValidator());
		}

		@Override
		protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
			if (this.webFluxRegistrations != null && this.webFluxRegistrations
					.getRequestMappingHandlerAdapter() != null) {
				return this.webFluxRegistrations.getRequestMappingHandlerAdapter();
			}
			return super.createRequestMappingHandlerAdapter();
		}

		@Override
		protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
			if (this.webFluxRegistrations != null && this.webFluxRegistrations
					.getRequestMappingHandlerMapping() != null) {
				return this.webFluxRegistrations.getRequestMappingHandlerMapping();
			}
			return super.createRequestMappingHandlerMapping();
		}

	}

	@Configuration
	@ConditionalOnEnabledResourceChain
	static class ResourceChainCustomizerConfiguration {

		@Bean
		public ResourceChainResourceHandlerRegistrationCustomizer resourceHandlerRegistrationCustomizer() {
			return new ResourceChainResourceHandlerRegistrationCustomizer();
		}

	}

}
