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

package org.springframework.boot.autoconfigure.thymeleaf;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;

import javax.annotation.PostConstruct;

import com.github.mxab.thymeleaf.extras.dataattribute.dialect.DataAttributeDialect;
import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.extras.springsecurity4.dialect.SpringSecurityDialect;
import org.thymeleaf.spring5.ISpringWebFluxTemplateEngine;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.SpringWebFluxTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.spring5.view.reactive.ThymeleafReactiveViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.template.TemplateLocation;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafProperties.Reactive;
import org.springframework.boot.autoconfigure.web.ConditionalOnEnabledResourceChain;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.MimeType;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Thymeleaf.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author Eddú Meléndez
 * @author Daniel Fernández
 * @author Kazuki Shimizu
 */
@Configuration
@EnableConfigurationProperties(ThymeleafProperties.class)
@ConditionalOnClass(TemplateMode.class)
@AutoConfigureAfter({ WebMvcAutoConfiguration.class, WebFluxAutoConfiguration.class })
public class ThymeleafAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(name = "defaultTemplateResolver")
	static class DefaultTemplateResolverConfiguration {

		private static final Log logger = LogFactory
				.getLog(DefaultTemplateResolverConfiguration.class);

		private final ThymeleafProperties properties;

		private final ApplicationContext applicationContext;

		DefaultTemplateResolverConfiguration(ThymeleafProperties properties,
				ApplicationContext applicationContext) {
			this.properties = properties;
			this.applicationContext = applicationContext;
		}

		@PostConstruct
		public void checkTemplateLocationExists() {
			boolean checkTemplateLocation = this.properties.isCheckTemplateLocation();
			if (checkTemplateLocation) {
				TemplateLocation location = new TemplateLocation(
						this.properties.getPrefix());
				if (!location.exists(this.applicationContext)) {
					logger.warn("Cannot find template location: " + location
							+ " (please add some templates or check "
							+ "your Thymeleaf configuration)");
				}
			}
		}

		@Bean
		public SpringResourceTemplateResolver defaultTemplateResolver() {
			SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
			resolver.setApplicationContext(this.applicationContext);
			resolver.setPrefix(this.properties.getPrefix());
			resolver.setSuffix(this.properties.getSuffix());
			resolver.setTemplateMode(this.properties.getMode());
			if (this.properties.getEncoding() != null) {
				resolver.setCharacterEncoding(this.properties.getEncoding().name());
			}
			resolver.setCacheable(this.properties.isCache());
			Integer order = this.properties.getTemplateResolverOrder();
			if (order != null) {
				resolver.setOrder(order);
			}
			resolver.setCheckExistence(this.properties.isCheckTemplate());
			return resolver;
		}

	}

	@Configuration
	protected static class ThymeleafDefaultConfiguration {

		private final ThymeleafProperties properties;

		private final Collection<ITemplateResolver> templateResolvers;

		private final Collection<IDialect> dialects;

		public ThymeleafDefaultConfiguration(ThymeleafProperties properties,
				Collection<ITemplateResolver> templateResolvers,
				ObjectProvider<Collection<IDialect>> dialectsProvider) {
			this.properties = properties;
			this.templateResolvers = templateResolvers;
			this.dialects = dialectsProvider.getIfAvailable(Collections::emptyList);
		}

		@Bean
		@ConditionalOnMissingBean
		public SpringTemplateEngine templateEngine() {
			SpringTemplateEngine engine = new SpringTemplateEngine();
			engine.setEnableSpringELCompiler(this.properties.isEnableSpringElCompiler());
			this.templateResolvers.forEach(engine::addTemplateResolver);
			this.dialects.forEach(engine::addDialect);
			return engine;
		}

	}

	@Configuration
	@ConditionalOnWebApplication(type = Type.SERVLET)
	@ConditionalOnProperty(name = "spring.thymeleaf.enabled", matchIfMissing = true)
	static class ThymeleafWebMvcConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnEnabledResourceChain
		public ResourceUrlEncodingFilter resourceUrlEncodingFilter() {
			return new ResourceUrlEncodingFilter();
		}

		@Configuration
		static class ThymeleafViewResolverConfiguration {

			private final ThymeleafProperties properties;

			private final SpringTemplateEngine templateEngine;

			ThymeleafViewResolverConfiguration(ThymeleafProperties properties,
					SpringTemplateEngine templateEngine) {
				this.properties = properties;
				this.templateEngine = templateEngine;
			}

			@Bean
			@ConditionalOnMissingBean(name = "thymeleafViewResolver")
			public ThymeleafViewResolver thymeleafViewResolver() {
				ThymeleafViewResolver resolver = new ThymeleafViewResolver();
				resolver.setTemplateEngine(this.templateEngine);
				resolver.setCharacterEncoding(this.properties.getEncoding().name());
				resolver.setContentType(
						appendCharset(this.properties.getServlet().getContentType(),
								resolver.getCharacterEncoding()));
				resolver.setExcludedViewNames(this.properties.getExcludedViewNames());
				resolver.setViewNames(this.properties.getViewNames());
				// This resolver acts as a fallback resolver (e.g. like a
				// InternalResourceViewResolver) so it needs to have low precedence
				resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 5);
				resolver.setCache(this.properties.isCache());
				return resolver;
			}

			private String appendCharset(MimeType type, String charset) {
				if (type.getCharset() != null) {
					return type.toString();
				}
				LinkedHashMap<String, String> parameters = new LinkedHashMap<>();
				parameters.put("charset", charset);
				parameters.putAll(type.getParameters());
				return new MimeType(type, parameters).toString();
			}

		}

	}

	@Configuration
	@ConditionalOnWebApplication(type = Type.REACTIVE)
	@ConditionalOnProperty(name = "spring.thymeleaf.enabled", matchIfMissing = true)
	static class ThymeleafReactiveConfiguration {

		private final ThymeleafProperties properties;

		private final Collection<ITemplateResolver> templateResolvers;

		private final Collection<IDialect> dialects;

		ThymeleafReactiveConfiguration(ThymeleafProperties properties,
				Collection<ITemplateResolver> templateResolvers,
				ObjectProvider<Collection<IDialect>> dialectsProvider) {
			this.properties = properties;
			this.templateResolvers = templateResolvers;
			this.dialects = dialectsProvider.getIfAvailable(Collections::emptyList);
		}

		@Bean
		@ConditionalOnMissingBean(ISpringWebFluxTemplateEngine.class)
		public SpringWebFluxTemplateEngine templateEngine() {
			SpringWebFluxTemplateEngine engine = new SpringWebFluxTemplateEngine();
			engine.setEnableSpringELCompiler(this.properties.isEnableSpringElCompiler());
			this.templateResolvers.forEach(engine::addTemplateResolver);
			this.dialects.forEach(engine::addDialect);
			return engine;
		}

	}

	@Configuration
	@ConditionalOnWebApplication(type = Type.REACTIVE)
	@ConditionalOnProperty(name = "spring.thymeleaf.enabled", matchIfMissing = true)
	static class ThymeleafWebFluxConfiguration {

		private final ThymeleafProperties properties;

		ThymeleafWebFluxConfiguration(ThymeleafProperties properties) {
			this.properties = properties;
		}

		@Bean
		@ConditionalOnMissingBean(name = "thymeleafReactiveViewResolver")
		public ThymeleafReactiveViewResolver thymeleafViewResolver(
				ISpringWebFluxTemplateEngine templateEngine) {
			ThymeleafReactiveViewResolver resolver = new ThymeleafReactiveViewResolver();
			resolver.setTemplateEngine(templateEngine);
			mapProperties(this.properties, resolver);
			mapReactiveProperties(this.properties.getReactive(), resolver);
			// This resolver acts as a fallback resolver (e.g. like a
			// InternalResourceViewResolver) so it needs to have low precedence
			resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 5);
			return resolver;
		}

		private void mapProperties(ThymeleafProperties properties,
				ThymeleafReactiveViewResolver resolver) {
			PropertyMapper map = PropertyMapper.get();
			map.from(properties::getEncoding).to(resolver::setDefaultCharset);
			resolver.setExcludedViewNames(properties.getExcludedViewNames());
			resolver.setViewNames(properties.getViewNames());
		}

		private void mapReactiveProperties(Reactive properties,
				ThymeleafReactiveViewResolver resolver) {
			PropertyMapper map = PropertyMapper.get();
			map.from(properties::getMediaTypes).whenNonNull()
					.to(resolver::setSupportedMediaTypes);
			map.from(properties::getMaxChunkSize).when((size) -> size > 0)
					.to(resolver::setResponseMaxChunkSizeBytes);
			map.from(properties::getFullModeViewNames).to(resolver::setFullModeViewNames);
			map.from(properties::getChunkedModeViewNames)
					.to(resolver::setChunkedModeViewNames);
		}

	}

	@Configuration
	@ConditionalOnClass(name = "nz.net.ultraq.thymeleaf.LayoutDialect")
	protected static class ThymeleafWebLayoutConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public LayoutDialect layoutDialect() {
			return new LayoutDialect();
		}

	}

	@Configuration
	@ConditionalOnClass(DataAttributeDialect.class)
	protected static class DataAttributeDialectConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public DataAttributeDialect dialect() {
			return new DataAttributeDialect();
		}

	}

	@Configuration
	@ConditionalOnClass({ SpringSecurityDialect.class })
	protected static class ThymeleafSecurityDialectConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public SpringSecurityDialect securityDialect() {
			return new SpringSecurityDialect();
		}

	}

	@Configuration
	@ConditionalOnClass(Java8TimeDialect.class)
	protected static class ThymeleafJava8TimeDialect {

		@Bean
		@ConditionalOnMissingBean
		public Java8TimeDialect java8TimeDialect() {
			return new Java8TimeDialect();
		}

	}

}
