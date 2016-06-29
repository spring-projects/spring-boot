/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.mobile;

import org.thymeleaf.spring4.view.ThymeleafViewResolver;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration;
import org.springframework.boot.autoconfigure.mustache.web.MustacheViewResolver;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mobile.device.view.LiteDeviceDelegatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.servlet.view.groovy.GroovyMarkupViewResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Mobile's
 * {@link LiteDeviceDelegatingViewResolver}. If {@link ThymeleafViewResolver} is available
 * it is configured as the delegate view resolver. Otherwise,
 * {@link InternalResourceViewResolver} is used as a fallback.
 *
 * @author Roy Clarkson
 * @author Stephane Nicoll
 * @since 1.1.0
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass(LiteDeviceDelegatingViewResolver.class)
@ConditionalOnProperty(prefix = "spring.mobile.devicedelegatingviewresolver", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(DeviceDelegatingViewResolverProperties.class)
@AutoConfigureAfter({ WebMvcAutoConfiguration.class, FreeMarkerAutoConfiguration.class,
		GroovyTemplateAutoConfiguration.class, MustacheAutoConfiguration.class,
		ThymeleafAutoConfiguration.class })
public class DeviceDelegatingViewResolverAutoConfiguration {

	@Configuration
	protected static class LiteDeviceDelegatingViewResolverFactoryConfiguration {

		@Bean
		public DeviceDelegatingViewResolverFactory deviceDelegatingViewResolverFactory(
				DeviceDelegatingViewResolverProperties properties) {
			return new DeviceDelegatingViewResolverFactory(properties);
		}

	}

	@Configuration
	@ConditionalOnClass(FreeMarkerViewResolver.class)
	protected static class DeviceDelegatingFreemarkerViewResolverConfiguration {

		@Bean
		@ConditionalOnBean(FreeMarkerViewResolver.class)
		public LiteDeviceDelegatingViewResolver deviceDelegatingFreemarkerViewResolver(
				DeviceDelegatingViewResolverFactory factory,
				FreeMarkerViewResolver viewResolver) {
			return factory.createViewResolver(viewResolver);
		}

	}

	@Configuration
	@ConditionalOnClass(GroovyMarkupViewResolver.class)
	protected static class DeviceDelegatingGroovyMarkupViewResolverConfiguration {

		@Bean
		@ConditionalOnBean(GroovyMarkupViewResolver.class)
		public LiteDeviceDelegatingViewResolver deviceDelegatingGroovyMarkupViewResolver(
				DeviceDelegatingViewResolverFactory factory,
				GroovyMarkupViewResolver viewResolver) {
			return factory.createViewResolver(viewResolver);
		}

	}

	@Configuration
	@ConditionalOnClass(InternalResourceViewResolver.class)
	protected static class DeviceDelegatingJspViewResolverConfiguration {

		@Bean
		@ConditionalOnBean(InternalResourceViewResolver.class)
		public LiteDeviceDelegatingViewResolver deviceDelegatingJspViewResolver(
				DeviceDelegatingViewResolverFactory factory,
				InternalResourceViewResolver viewResolver) {
			return factory.createViewResolver(viewResolver);
		}

	}

	@Configuration
	@ConditionalOnClass(MustacheViewResolver.class)
	protected static class DeviceDelegatingMustacheViewResolverConfiguration {

		@Bean
		@ConditionalOnBean(MustacheViewResolver.class)
		public LiteDeviceDelegatingViewResolver deviceDelegatingMustacheViewResolver(
				DeviceDelegatingViewResolverFactory factory,
				MustacheViewResolver viewResolver) {
			return factory.createViewResolver(viewResolver);
		}

	}

	@Configuration
	@ConditionalOnClass(ThymeleafViewResolver.class)
	protected static class DeviceDelegatingThymeleafViewResolverConfiguration {

		@Bean
		@ConditionalOnBean(ThymeleafViewResolver.class)
		public LiteDeviceDelegatingViewResolver deviceDelegatingThymeleafViewResolver(
				DeviceDelegatingViewResolverFactory factory,
				ThymeleafViewResolver viewResolver) {
			return factory.createViewResolver(viewResolver);
		}

	}

}
