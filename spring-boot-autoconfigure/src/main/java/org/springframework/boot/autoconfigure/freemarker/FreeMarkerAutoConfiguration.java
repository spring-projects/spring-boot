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

package org.springframework.boot.autoconfigure.freemarker;

import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.servlet.Servlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;
import org.springframework.util.Assert;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfig;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for FreeMarker.
 * 
 * @author Andy Wilkinson
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass(freemarker.template.Configuration.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class FreeMarkerAutoConfiguration implements EnvironmentAware {

	public static final String DEFAULT_TEMPLATE_LOADER_PATH = "classpath:/templates/";

	public static final String DEFAULT_PREFIX = "";

	public static final String DEFAULT_SUFFIX = ".ftl";

	@Autowired
	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

	private RelaxedPropertyResolver environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = new RelaxedPropertyResolver(environment, "spring.freeMarker.");
	}

	@PostConstruct
	public void checkTemplateLocationExists() {
		Boolean checkTemplateLocation = this.environment.getProperty(
				"checkTemplateLocation", Boolean.class, true);
		if (checkTemplateLocation) {
			Resource resource = this.resourceLoader.getResource(this.environment
					.getProperty("templateLoaderPath", DEFAULT_TEMPLATE_LOADER_PATH));
			Assert.state(resource.exists(), "Cannot find template location: " + resource
					+ " (please add some templates "
					+ "or check your FreeMarker configuration)");
		}
	}

	@Configuration
	@ConditionalOnNotWebApplication
	public static class FreeMarkerNonWebConfiguration implements EnvironmentAware {

		private RelaxedPropertyResolver environment;

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = new RelaxedPropertyResolver(environment,
					"spring.freemarker.");
		}

		@Bean
		@ConditionalOnMissingBean
		public FreeMarkerConfigurationFactoryBean freeMarkerConfigurer() {
			FreeMarkerConfigurationFactoryBean freeMarkerConfigurer = new FreeMarkerConfigurationFactoryBean();
			freeMarkerConfigurer.setTemplateLoaderPath(this.environment.getProperty(
					"templateLoaderPath", DEFAULT_TEMPLATE_LOADER_PATH));
			freeMarkerConfigurer.setDefaultEncoding(this.environment.getProperty(
					"templateEncoding", "UTF-8"));
			Map<String, Object> settingsMap = this.environment
					.getSubProperties("settings.");
			Properties settings = new Properties();
			settings.putAll(settingsMap);
			freeMarkerConfigurer.setFreemarkerSettings(settings);
			return freeMarkerConfigurer;
		}

	}

	@Configuration
	@ConditionalOnClass(Servlet.class)
	@ConditionalOnWebApplication
	public static class FreeMarkerWebConfiguration implements EnvironmentAware {

		private RelaxedPropertyResolver environment;

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = new RelaxedPropertyResolver(environment,
					"spring.freemarker.");
		}

		@Bean
		@ConditionalOnMissingBean
		public FreeMarkerConfigurer freeMarkerConfigurer() {
			FreeMarkerConfigurer freeMarkerConfigurer = new FreeMarkerConfigurer();
			freeMarkerConfigurer.setTemplateLoaderPath(this.environment.getProperty(
					"templateLoaderPath", DEFAULT_TEMPLATE_LOADER_PATH));
			freeMarkerConfigurer.setDefaultEncoding(this.environment.getProperty(
					"templateEncoding", "UTF-8"));
			Map<String, Object> settingsMap = this.environment
					.getSubProperties("settings.");
			Properties settings = new Properties();
			settings.putAll(settingsMap);
			freeMarkerConfigurer.setFreemarkerSettings(settings);
			return freeMarkerConfigurer;
		}

		@Bean
		@ConditionalOnBean(FreeMarkerConfigurer.class)
		@ConditionalOnMissingBean
		public freemarker.template.Configuration freemarkerConfiguration(
				FreeMarkerConfig configurer) {
			return configurer.getConfiguration();
		}

		@Bean
		@ConditionalOnMissingBean(name = "freeMarkerViewResolver")
		public FreeMarkerViewResolver freeMarkerViewResolver() {
			FreeMarkerViewResolver resolver = new FreeMarkerViewResolver();
			resolver.setPrefix(this.environment.getProperty("prefix", DEFAULT_PREFIX));
			resolver.setSuffix(this.environment.getProperty("suffix", DEFAULT_SUFFIX));
			resolver.setCache(this.environment.getProperty("cache", Boolean.class, true));
			resolver.setContentType(this.environment.getProperty("contentType",
					"text/html"));
			resolver.setViewNames(this.environment.getProperty("viewNames",
					String[].class));
			resolver.setExposeRequestAttributes(this.environment.getProperty(
					"exposeRequestAttributes", Boolean.class, false));
			resolver.setAllowRequestOverride(this.environment.getProperty(
					"allowRequestOverride", Boolean.class, false));
			resolver.setExposeSessionAttributes(this.environment.getProperty(
					"exposeSessionAttributes", Boolean.class, false));
			resolver.setAllowSessionOverride(this.environment.getProperty(
					"allowSessionOverride", Boolean.class, false));
			resolver.setExposeSpringMacroHelpers(this.environment.getProperty(
					"exposeSpringMacroHelpers", Boolean.class, true));
			resolver.setRequestContextAttribute(this.environment
					.getProperty("requestContextAttribute"));

			// This resolver acts as a fallback resolver (e.g. like a
			// InternalResourceViewResolver) so it needs to have low precedence
			resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 5);

			return resolver;
		}
	}
}
