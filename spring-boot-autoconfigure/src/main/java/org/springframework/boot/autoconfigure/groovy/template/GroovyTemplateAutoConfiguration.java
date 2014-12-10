/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.groovy.template;

import groovy.text.markup.MarkupTemplateEngine;

import javax.annotation.PostConstruct;
import javax.servlet.Servlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.web.servlet.view.UrlBasedViewResolver;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfig;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfigurer;
import org.springframework.web.servlet.view.groovy.GroovyMarkupViewResolver;

/**
 * Autoconfiguration support for Groovy templates in MVC. By default creates a
 * {@link MarkupTemplateEngine} configured from {@link GroovyTemplateProperties}, but you
 * can override that by providing your own {@link GroovyMarkupConfig} or even a
 * {@link MarkupTemplateEngine} of a different type.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass(MarkupTemplateEngine.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
@EnableConfigurationProperties(GroovyTemplateProperties.class)
public class GroovyTemplateAutoConfiguration {

	@Configuration
	@ConditionalOnClass(GroovyMarkupConfigurer.class)
	public static class GroovyMarkupConfiguration {

		@Autowired
		private final ResourceLoader resourceLoader = new DefaultResourceLoader();

		@Autowired
		private GroovyTemplateProperties properties;

		@Autowired(required = false)
		private MarkupTemplateEngine templateEngine;

		@PostConstruct
		public void checkTemplateLocationExists() {
			if (this.properties.isCheckTemplateLocation()) {
				Resource resource = this.resourceLoader.getResource(this.properties
						.getPrefix());
				Assert.state(resource.exists(), "Cannot find template location: "
						+ resource + " (please add some templates, "
						+ "check your Groovy configuration, or set "
						+ "spring.groovy.template.check-template-location=false)");
			}
		}

		@Bean
		@ConditionalOnMissingBean(GroovyMarkupConfig.class)
		@ConfigurationProperties(prefix = "spring.groovy.template.configuration")
		public GroovyMarkupConfigurer groovyMarkupConfigurer() {
			GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
			configurer.setResourceLoaderPath(this.properties.getPrefix());
			configurer.setCacheTemplates(this.properties.isCache());
			if (this.templateEngine != null) {
				configurer.setTemplateEngine(this.templateEngine);
			}
			return configurer;
		}

	}

	@Configuration
	@ConditionalOnClass({ Servlet.class, LocaleContextHolder.class,
			UrlBasedViewResolver.class })
	@ConditionalOnWebApplication
	@ConditionalOnProperty(name = "spring.groovy.template.enabled", matchIfMissing = true)
	public static class GroovyWebConfiguration {

		@Autowired
		private GroovyTemplateProperties properties;

		@Bean
		@ConditionalOnMissingBean(name = "groovyMarkupViewResolver")
		public GroovyMarkupViewResolver groovyMarkupViewResolver() {
			GroovyMarkupViewResolver resolver = new GroovyMarkupViewResolver();
			configureViewResolver(resolver);
			return resolver;
		}

		private void configureViewResolver(UrlBasedViewResolver resolver) {
			resolver.setSuffix(this.properties.getSuffix());
			resolver.setCache(this.properties.isCache());
			resolver.setContentType(this.properties.getContentType());
			resolver.setViewNames(this.properties.getViewNames());
			resolver.setRequestContextAttribute("spring");
			// This resolver acts as a fallback resolver (e.g. like a
			// InternalResourceViewResolver) so it needs to have low precedence
			resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 6);
		}

	}

}
