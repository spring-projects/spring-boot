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

package org.springframework.boot.autoconfigure.velocity;

import java.io.IOException;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.servlet.Servlet;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.ui.velocity.VelocityEngineFactory;
import org.springframework.ui.velocity.VelocityEngineFactoryBean;
import org.springframework.util.Assert;
import org.springframework.web.servlet.view.velocity.VelocityConfig;
import org.springframework.web.servlet.view.velocity.VelocityConfigurer;
import org.springframework.web.servlet.view.velocity.VelocityViewResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Velocity.
 * 
 * @author Andy Wilkinson
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass(VelocityEngine.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
@EnableConfigurationProperties(VelocityProperties.class)
public class VelocityAutoConfiguration {

	@Autowired
	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

	@Autowired
	private VelocityProperties properties;

	@PostConstruct
	public void checkTemplateLocationExists() {
		if (this.properties.isCheckTemplateLocation()) {
			Resource resource = this.resourceLoader.getResource(this.properties
					.getResourceLoaderPath());
			Assert.state(resource.exists(), "Cannot find template location: " + resource
					+ " (please add some templates, check your Velocity configuration, "
					+ "or set spring.velocity.checkTemplateLocation=false)");
		}
	}

	protected static class VelocityConfiguration {

		@Autowired
		protected VelocityProperties properties;

		protected void applyProperties(VelocityEngineFactory factory) {
			factory.setResourceLoaderPath(this.properties.getResourceLoaderPath());
			Properties velocityProperties = new Properties();
			velocityProperties.putAll(this.properties.getProperties());
			factory.setVelocityProperties(velocityProperties);
		}

	}

	@Configuration
	@ConditionalOnNotWebApplication
	public static class VelocityNonWebConfiguration extends VelocityConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public VelocityEngineFactoryBean velocityConfiguration() {
			VelocityEngineFactoryBean velocityEngineFactoryBean = new VelocityEngineFactoryBean();
			applyProperties(velocityEngineFactoryBean);
			return velocityEngineFactoryBean;
		}

	}

	@Configuration
	@ConditionalOnClass(Servlet.class)
	@ConditionalOnWebApplication
	public static class VelocityWebConfiguration extends VelocityConfiguration {

		@Bean
		@ConditionalOnMissingBean(VelocityConfig.class)
		public VelocityConfigurer velocityConfigurer() {
			VelocityConfigurer configurer = new VelocityConfigurer();
			applyProperties(configurer);
			return configurer;
		}

		@Bean
		public VelocityEngine velocityEngine(VelocityConfigurer configurer)
				throws VelocityException, IOException {
			return configurer.createVelocityEngine();
		}

		@Bean
		@ConditionalOnMissingBean(name = "velocityViewResolver")
		public VelocityViewResolver velocityViewResolver() {
			VelocityViewResolver resolver = new VelocityViewResolver();
			this.properties.applyToViewResolver(resolver);
			return resolver;
		}

	}

}
