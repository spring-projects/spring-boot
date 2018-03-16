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
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidatorAdapter;
import org.springframework.boot.autoconfigure.web.ConditionalOnEnabledResourceChain;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.format.WebConversionService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.CacheControl;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.config.DelegatingWebFluxConfiguration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceChainRegistration;
import org.springframework.web.reactive.config.ResourceHandlerRegistration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurationSupport;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.resource.AppCacheManifestTransformer;
import org.springframework.web.reactive.resource.GzipResourceResolver;
import org.springframework.web.reactive.resource.ResourceResolver;
import org.springframework.web.reactive.resource.VersionResourceResolver;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
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

	@Configuration
	@EnableConfigurationProperties({ ResourceProperties.class, WebFluxProperties.class })
	@Import({ EnableWebFluxConfiguration.class })
	public static class WebFluxConfig implements WebFluxConfigurer {

		private static final Log logger = LogFactory.getLog(WebFluxConfig.class);

		private final ResourceProperties resourceProperties;

		private final WebFluxProperties webFluxProperties;

		private final ListableBeanFactory beanFactory;

		private final List<HandlerMethodArgumentResolver> argumentResolvers;

		private final List<CodecCustomizer> codecCustomizers;

		private final ResourceHandlerRegistrationCustomizer resourceHandlerRegistrationCustomizer;

		private final List<ViewResolver> viewResolvers;

		public WebFluxConfig(ResourceProperties resourceProperties,
				WebFluxProperties webFluxProperties, ListableBeanFactory beanFactory,
				ObjectProvider<List<HandlerMethodArgumentResolver>> resolvers,
				ObjectProvider<List<CodecCustomizer>> codecCustomizers,
				ObjectProvider<ResourceHandlerRegistrationCustomizer> resourceHandlerRegistrationCustomizer,
				ObjectProvider<List<ViewResolver>> viewResolvers) {
			this.resourceProperties = resourceProperties;
			this.webFluxProperties = webFluxProperties;
			this.beanFactory = beanFactory;
			this.argumentResolvers = resolvers.getIfAvailable();
			this.codecCustomizers = codecCustomizers.getIfAvailable();
			this.resourceHandlerRegistrationCustomizer = resourceHandlerRegistrationCustomizer
					.getIfAvailable();
			this.viewResolvers = viewResolvers.getIfAvailable();
		}

		@Override
		public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
			if (this.argumentResolvers != null) {
				this.argumentResolvers.forEach(configurer::addCustomResolver);
			}
		}

		@Override
		public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
			if (this.codecCustomizers != null) {
				this.codecCustomizers
						.forEach((customizer) -> customizer.customize(configurer));
			}
		}

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			if (!this.resourceProperties.isAddMappings()) {
				logger.debug("Default resource handling disabled");
				return;
			}
			Duration cachePeriod = this.resourceProperties.getCache().getPeriod();
			if (!registry.hasMappingForPattern("/webjars/**")) {
				ResourceHandlerRegistration registration = registry
						.addResourceHandler("/webjars/**")
						.addResourceLocations("classpath:/META-INF/resources/webjars/");
				if (cachePeriod != null) {
					registration.setCacheControl(CacheControl
							.maxAge(cachePeriod.toMillis(), TimeUnit.MILLISECONDS));
				}
				customizeResourceHandlerRegistration(registration);
			}
			String staticPathPattern = this.webFluxProperties.getStaticPathPattern();
			if (!registry.hasMappingForPattern(staticPathPattern)) {
				ResourceHandlerRegistration registration = registry
						.addResourceHandler(staticPathPattern).addResourceLocations(
								this.resourceProperties.getStaticLocations());
				if (cachePeriod != null) {
					registration.setCacheControl(CacheControl
							.maxAge(cachePeriod.toMillis(), TimeUnit.MILLISECONDS));
				}
				customizeResourceHandlerRegistration(registration);
			}
		}

		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			if (this.viewResolvers != null) {
				AnnotationAwareOrderComparator.sort(this.viewResolvers);
				this.viewResolvers.forEach(registry::viewResolver);
			}
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

		public EnableWebFluxConfiguration(WebFluxProperties webFluxProperties) {
			this.webFluxProperties = webFluxProperties;
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

	}

	@Configuration
	@ConditionalOnEnabledResourceChain
	static class ResourceChainCustomizerConfiguration {

		@Bean
		public ResourceChainResourceHandlerRegistrationCustomizer resourceHandlerRegistrationCustomizer() {
			return new ResourceChainResourceHandlerRegistrationCustomizer();
		}

	}

	interface ResourceHandlerRegistrationCustomizer {

		void customize(ResourceHandlerRegistration registration);

	}

	private static class ResourceChainResourceHandlerRegistrationCustomizer
			implements ResourceHandlerRegistrationCustomizer {

		@Autowired
		private ResourceProperties resourceProperties = new ResourceProperties();

		@Override
		public void customize(ResourceHandlerRegistration registration) {
			ResourceProperties.Chain properties = this.resourceProperties.getChain();
			configureResourceChain(properties,
					registration.resourceChain(properties.isCache()));
		}

		private void configureResourceChain(ResourceProperties.Chain properties,
				ResourceChainRegistration chain) {
			ResourceProperties.Strategy strategy = properties.getStrategy();
			if (strategy.getFixed().isEnabled() || strategy.getContent().isEnabled()) {
				chain.addResolver(getVersionResourceResolver(strategy));
			}
			if (properties.isGzipped()) {
				chain.addResolver(new GzipResourceResolver());
			}
			if (properties.isHtmlApplicationCache()) {
				chain.addTransformer(new AppCacheManifestTransformer());
			}
		}

		private ResourceResolver getVersionResourceResolver(
				ResourceProperties.Strategy properties) {
			VersionResourceResolver resolver = new VersionResourceResolver();
			if (properties.getFixed().isEnabled()) {
				String version = properties.getFixed().getVersion();
				String[] paths = properties.getFixed().getPaths();
				resolver.addFixedVersionStrategy(version, paths);
			}
			if (properties.getContent().isEnabled()) {
				String[] paths = properties.getContent().getPaths();
				resolver.addContentVersionStrategy(paths);
			}
			return resolver;
		}

	}

}
