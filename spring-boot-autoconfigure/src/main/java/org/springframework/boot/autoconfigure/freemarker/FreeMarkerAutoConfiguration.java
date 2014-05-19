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

import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.servlet.Servlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.template.TemplateViewResolverConfigurer;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;
import org.springframework.util.Assert;
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
@ConditionalOnClass(freemarker.template.Configuration.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
@EnableConfigurationProperties(FreeMarkerProperties.class)
public class FreeMarkerAutoConfiguration {

	@Autowired
	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

	@Autowired
	private FreeMarkerProperties properties;

	@PostConstruct
	public void checkTemplateLocationExists() {
		if (this.properties.isCheckTemplateLocation()) {
			Resource resource = this.resourceLoader.getResource(this.properties
					.getTemplateLoaderPath());
			Assert.state(
					resource.exists(),
					"Cannot find template location: "
							+ resource
							+ " (please add some templates, check your FreeMarker configuration, or set "
							+ "spring.freemarker.checkTemplateLocation=false)");
		}
	}

	protected static class FreeMarkerConfiguration {

		@Autowired
		protected FreeMarkerProperties properties;

		protected void applyProperties(FreeMarkerConfigurationFactory factory) {
			factory.setTemplateLoaderPath(this.properties.getTemplateLoaderPath());
			factory.setDefaultEncoding(this.properties.getCharSet());
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
		public FreeMarkerViewResolver freeMarkerViewResolver() {
			FreeMarkerViewResolver resolver = new FreeMarkerViewResolver();
			new TemplateViewResolverConfigurer().configureTemplateViewResolver(resolver,
					this.properties);
			return resolver;
		}

	}
}
