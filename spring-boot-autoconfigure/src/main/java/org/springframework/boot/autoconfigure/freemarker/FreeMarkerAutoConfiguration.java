/*
 * Copyright 2012-2015 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.servlet.Servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.template.TemplateLocation;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfig;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for FreeMarker.
 *
 * @author Andy Wilkinson
 * @author Dave Syer
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass({ freemarker.template.Configuration.class,
		FreeMarkerConfigurationFactory.class })
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
@EnableConfigurationProperties(FreeMarkerProperties.class)
public class FreeMarkerAutoConfiguration {

	private static final Log logger = LogFactory
			.getLog(FreeMarkerAutoConfiguration.class);

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private FreeMarkerProperties properties;

	@PostConstruct
	public void checkTemplateLocationExists() {
		if (this.properties.isCheckTemplateLocation()) {
			TemplateLocation templatePathLocation = null;
			List<TemplateLocation> locations = new ArrayList<TemplateLocation>();
			for (String templateLoaderPath : this.properties.getTemplateLoaderPath()) {
				TemplateLocation location = new TemplateLocation(templateLoaderPath);
				locations.add(location);
				if (location.exists(this.applicationContext)) {
					templatePathLocation = location;
					break;
				}
			}
			if (templatePathLocation == null) {
				logger.warn("Cannot find template location(s): " + locations
						+ " (please add some templates, "
						+ "check your FreeMarker configuration, or set "
						+ "spring.freemarker.checkTemplateLocation=false)");
			}
		}
	}

	protected static class FreeMarkerConfiguration {

		@Autowired
		protected FreeMarkerProperties properties;

		protected void applyProperties(FreeMarkerConfigurationFactory factory) {
			factory.setTemplateLoaderPaths(this.properties.getTemplateLoaderPath());
			factory.setPreferFileSystemAccess(this.properties.isPreferFileSystemAccess());
			factory.setDefaultEncoding(this.properties.getCharsetName());
			Properties settings = new Properties();
			settings.putAll(this.properties.getSettings());
			factory.setFreemarkerSettings(settings);
		}

	}

	@Configuration
	@ConditionalOnNotWebApplication
	public static class FreeMarkerNonWebConfiguration extends FreeMarkerConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public FreeMarkerConfigurationFactoryBean freeMarkerConfiguration() {
			FreeMarkerConfigurationFactoryBean freeMarkerFactoryBean = new FreeMarkerConfigurationFactoryBean();
			applyProperties(freeMarkerFactoryBean);
			return freeMarkerFactoryBean;
		}

	}

	@Configuration
	@ConditionalOnClass(Servlet.class)
	@ConditionalOnWebApplication
	public static class FreeMarkerWebConfiguration extends FreeMarkerConfiguration {

		@Bean
		@ConditionalOnMissingBean(FreeMarkerConfig.class)
		public FreeMarkerConfigurer freeMarkerConfigurer() {
			FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
			applyProperties(configurer);
			return configurer;
		}

		@Bean
		public freemarker.template.Configuration freeMarkerConfiguration(
				FreeMarkerConfig configurer) {
			return configurer.getConfiguration();
		}

		@Bean
		@ConditionalOnMissingBean(name = "freeMarkerViewResolver")
		@ConditionalOnProperty(name = "spring.freemarker.enabled", matchIfMissing = true)
		public FreeMarkerViewResolver freeMarkerViewResolver() {
			FreeMarkerViewResolver resolver = new FreeMarkerViewResolver();
			this.properties.applyToViewResolver(resolver);
			return resolver;
		}

	}
}
