/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.mappings;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.web.mappings.MappingDescriptionProvider;
import org.springframework.boot.actuate.web.mappings.MappingsEndpoint;
import org.springframework.boot.actuate.web.mappings.reactive.DispatcherHandlersMappingDescriptionProvider;
import org.springframework.boot.actuate.web.mappings.servlet.DispatcherServletsMappingDescriptionProvider;
import org.springframework.boot.actuate.web.mappings.servlet.FiltersMappingDescriptionProvider;
import org.springframework.boot.actuate.web.mappings.servlet.ServletsMappingDescriptionProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link MappingsEndpoint}.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnAvailableEndpoint(endpoint = MappingsEndpoint.class)
public class MappingsEndpointAutoConfiguration {

	@Bean
	public MappingsEndpoint mappingsEndpoint(ApplicationContext applicationContext,
			ObjectProvider<MappingDescriptionProvider> descriptionProviders) {
		return new MappingsEndpoint(descriptionProviders.orderedStream().toList(), applicationContext);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = Type.SERVLET)
	static class ServletWebConfiguration {

		@Bean
		ServletsMappingDescriptionProvider servletMappingDescriptionProvider() {
			return new ServletsMappingDescriptionProvider();
		}

		@Bean
		FiltersMappingDescriptionProvider filterMappingDescriptionProvider() {
			return new FiltersMappingDescriptionProvider();
		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass(DispatcherServlet.class)
		@ConditionalOnBean(DispatcherServlet.class)
		static class SpringMvcConfiguration {

			@Bean
			DispatcherServletsMappingDescriptionProvider dispatcherServletMappingDescriptionProvider() {
				return new DispatcherServletsMappingDescriptionProvider();
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = Type.REACTIVE)
	@ConditionalOnClass(DispatcherHandler.class)
	@ConditionalOnBean(DispatcherHandler.class)
	static class ReactiveWebConfiguration {

		@Bean
		DispatcherHandlersMappingDescriptionProvider dispatcherHandlerMappingDescriptionProvider() {
			return new DispatcherHandlersMappingDescriptionProvider();
		}

	}

}
