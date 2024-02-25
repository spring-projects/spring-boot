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

package org.springframework.boot.autoconfigure.groovy.template;

import java.security.CodeSource;
import java.security.ProtectionDomain;

import groovy.text.markup.MarkupTemplateEngine;
import jakarta.servlet.Servlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
import org.springframework.core.log.LogMessage;
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
@AutoConfiguration(after = WebMvcAutoConfiguration.class)
@ConditionalOnClass(MarkupTemplateEngine.class)
@EnableConfigurationProperties(GroovyTemplateProperties.class)
public class GroovyTemplateAutoConfiguration {

	private static final Log logger = LogFactory.getLog(GroovyTemplateAutoConfiguration.class);

	/**
	 * GroovyMarkupConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(GroovyMarkupConfigurer.class)
	public static class GroovyMarkupConfiguration {

		private final ApplicationContext applicationContext;

		private final GroovyTemplateProperties properties;

		/**
		 * Constructs a new GroovyMarkupConfiguration with the specified
		 * ApplicationContext and GroovyTemplateProperties.
		 * @param applicationContext the ApplicationContext to be used
		 * @param properties the GroovyTemplateProperties to be used
		 */
		public GroovyMarkupConfiguration(ApplicationContext applicationContext, GroovyTemplateProperties properties) {
			this.applicationContext = applicationContext;
			this.properties = properties;
			checkTemplateLocationExists();
		}

		/**
		 * Checks if the template location exists.
		 *
		 * This method checks if the template location exists by verifying the following
		 * conditions: - The property "checkTemplateLocation" is set to true. - The
		 * "groovy-all" jar is not being used.
		 *
		 * If the above conditions are met, a TemplateLocation object is created using the
		 * resource loader path specified in the properties. The existence of the template
		 * location is then checked using the application context. If the template
		 * location does not exist, a warning message is logged.
		 *
		 * Note: If templates are not found, it is recommended to add some templates,
		 * check the Groovy configuration, or set the property
		 * "spring.groovy.template.check-template-location" to false.
		 */
		public void checkTemplateLocationExists() {
			if (this.properties.isCheckTemplateLocation() && !isUsingGroovyAllJar()) {
				TemplateLocation location = new TemplateLocation(this.properties.getResourceLoaderPath());
				if (!location.exists(this.applicationContext)) {
					logger.warn(LogMessage.format(
							"Cannot find template location: %s (please add some templates, check your Groovy "
									+ "configuration, or set spring.groovy.template.check-template-location=false)",
							location));
				}
			}
		}

		/**
		 * MarkupTemplateEngine could be loaded from groovy-templates or groovy-all.
		 * Unfortunately it's quite common for people to use groovy-all and not actually
		 * need templating support. This method attempts to check the source jar so that
		 * we can skip the {@code /template} directory check for such cases.
		 * @return true if the groovy-all jar is used
		 */
		private boolean isUsingGroovyAllJar() {
			try {
				ProtectionDomain domain = MarkupTemplateEngine.class.getProtectionDomain();
				CodeSource codeSource = domain.getCodeSource();
				return codeSource != null && codeSource.getLocation().toString().contains("-all");
			}
			catch (Exception ex) {
				return false;
			}
		}

		/**
		 * Creates and configures a {@link GroovyMarkupConfigurer} bean if a bean of type
		 * {@link GroovyMarkupConfig} is not already present. The configuration properties
		 * are read from the "spring.groovy.template.configuration" prefix. The resource
		 * loader path is set based on the properties. The template caching is enabled or
		 * disabled based on the properties. If a {@link MarkupTemplateEngine} bean is
		 * available, it is set as the template engine for the configurer.
		 * @param templateEngine an object provider for {@link MarkupTemplateEngine} bean
		 * @return the configured {@link GroovyMarkupConfigurer} bean
		 */
		@Bean
		@ConditionalOnMissingBean(GroovyMarkupConfig.class)
		@ConfigurationProperties(prefix = "spring.groovy.template.configuration")
		public GroovyMarkupConfigurer groovyMarkupConfigurer(ObjectProvider<MarkupTemplateEngine> templateEngine) {
			GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
			configurer.setResourceLoaderPath(this.properties.getResourceLoaderPath());
			configurer.setCacheTemplates(this.properties.isCache());
			templateEngine.ifAvailable(configurer::setTemplateEngine);
			return configurer;
		}

	}

	/**
	 * GroovyWebConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ Servlet.class, LocaleContextHolder.class, UrlBasedViewResolver.class })
	@ConditionalOnWebApplication(type = Type.SERVLET)
	@ConditionalOnProperty(name = "spring.groovy.template.enabled", matchIfMissing = true)
	public static class GroovyWebConfiguration {

		/**
		 * Creates a GroovyMarkupViewResolver bean if there is no existing bean with the
		 * name "groovyMarkupViewResolver". The GroovyTemplateProperties are applied to
		 * the created GroovyMarkupViewResolver.
		 * @param properties the GroovyTemplateProperties to be applied to the
		 * GroovyMarkupViewResolver
		 * @return the created GroovyMarkupViewResolver bean
		 */
		@Bean
		@ConditionalOnMissingBean(name = "groovyMarkupViewResolver")
		public GroovyMarkupViewResolver groovyMarkupViewResolver(GroovyTemplateProperties properties) {
			GroovyMarkupViewResolver resolver = new GroovyMarkupViewResolver();
			properties.applyToMvcViewResolver(resolver);
			return resolver;
		}

	}

}
