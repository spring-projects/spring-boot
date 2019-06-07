/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.groovy.template;

import java.security.CodeSource;
import java.security.ProtectionDomain;

import javax.annotation.PostConstruct;
import javax.servlet.Servlet;

import groovy.text.markup.MarkupTemplateEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.template.TemplateLocation;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.view.UrlBasedViewResolver;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfig;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfigurer;
import org.springframework.web.servlet.view.groovy.GroovyMarkupViewResolver;

/**
 * Auto-configuration support for Groovy templates in MVC. By default creates a
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

	private static final Log logger = LogFactory.getLog(GroovyTemplateAutoConfiguration.class);

	@Configuration
	@ConditionalOnClass(GroovyMarkupConfigurer.class)
	public static class GroovyMarkupConfiguration {

		private final ApplicationContext applicationContext;

		private final GroovyTemplateProperties properties;

		private final MarkupTemplateEngine templateEngine;

		public GroovyMarkupConfiguration(ApplicationContext applicationContext, GroovyTemplateProperties properties,
				ObjectProvider<MarkupTemplateEngine> templateEngine) {
			this.applicationContext = applicationContext;
			this.properties = properties;
			this.templateEngine = templateEngine.getIfAvailable();
		}

		@PostConstruct
		public void checkTemplateLocationExists() {
			if (this.properties.isCheckTemplateLocation() && !isUsingGroovyAllJar()) {
				TemplateLocation location = new TemplateLocation(this.properties.getResourceLoaderPath());
				if (!location.exists(this.applicationContext)) {
					logger.warn("Cannot find template location: " + location
							+ " (please add some templates, check your Groovy "
							+ "configuration, or set spring.groovy.template." + "check-template-location=false)");
				}
			}
		}

		/**
		 * MarkupTemplateEngine could be loaded from groovy-templates or groovy-all.
		 * Unfortunately it's quite common for people to use groovy-all and not actually
		 * need templating support. This method check attempts to check the source jar so
		 * that we can skip the {@code /template} folder check for such cases.
		 * @return true if the groovy-all jar is used
		 */
		private boolean isUsingGroovyAllJar() {
			try {
				ProtectionDomain domain = MarkupTemplateEngine.class.getProtectionDomain();
				CodeSource codeSource = domain.getCodeSource();
				if (codeSource != null && codeSource.getLocation().toString().contains("-all")) {
					return true;
				}
				return false;
			}
			catch (Exception ex) {
				return false;
			}
		}

		@Bean
		@ConditionalOnMissingBean(GroovyMarkupConfig.class)
		@ConfigurationProperties(prefix = "spring.groovy.template.configuration")
		public GroovyMarkupConfigurer groovyMarkupConfigurer() {
			GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
			configurer.setResourceLoaderPath(this.properties.getResourceLoaderPath());
			configurer.setCacheTemplates(this.properties.isCache());
			if (this.templateEngine != null) {
				configurer.setTemplateEngine(this.templateEngine);
			}
			return configurer;
		}

	}

	@Configuration
	@ConditionalOnClass({ Servlet.class, LocaleContextHolder.class, UrlBasedViewResolver.class })
	@ConditionalOnWebApplication(type = Type.SERVLET)
	@ConditionalOnProperty(name = "spring.groovy.template.enabled", matchIfMissing = true)
	public static class GroovyWebConfiguration {

		private final GroovyTemplateProperties properties;

		public GroovyWebConfiguration(GroovyTemplateProperties properties) {
			this.properties = properties;
		}

		@Bean
		@ConditionalOnMissingBean(name = "groovyMarkupViewResolver")
		public GroovyMarkupViewResolver groovyMarkupViewResolver() {
			GroovyMarkupViewResolver resolver = new GroovyMarkupViewResolver();
			this.properties.applyToMvcViewResolver(resolver);
			return resolver;
		}

	}

}
