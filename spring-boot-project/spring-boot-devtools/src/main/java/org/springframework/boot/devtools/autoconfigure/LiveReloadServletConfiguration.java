/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.devtools.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.devtools.livereload.LiveReloadScriptInjectingFilter;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Servlet-specific local LiveReload configuration.
 *
 * @author Vedran Pavic
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
class LiveReloadServletConfiguration {

	@Bean
	@RestartScope
	LiveReloadScriptInjectingFilter liveReloadScriptInjectingFilter(DevToolsProperties properties) {
		return new LiveReloadScriptInjectingFilter(properties.getLivereload().getPort());
	}

	@Configuration(proxyBeanMethods = false)
	static class LiveReloadResourcesConfiguration implements WebMvcConfigurer {

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			ResourceHandlerRegistration registration = registry.addResourceHandler("/livereload.js");
			registration.addResourceLocations("classpath:/org/springframework/boot/devtools/livereload/");
		}

	}

}
