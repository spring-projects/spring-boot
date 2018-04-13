/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.web.mappings;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.junit.Test;

import org.springframework.boot.actuate.web.mappings.MappingsEndpoint.ApplicationMappings;
import org.springframework.boot.actuate.web.mappings.MappingsEndpoint.ContextMappings;
import org.springframework.boot.actuate.web.mappings.reactive.DispatcherHandlerMappingDescription;
import org.springframework.boot.actuate.web.mappings.reactive.DispatcherHandlersMappingDescriptionProvider;
import org.springframework.boot.actuate.web.mappings.servlet.DispatcherServletMappingDescription;
import org.springframework.boot.actuate.web.mappings.servlet.DispatcherServletsMappingDescriptionProvider;
import org.springframework.boot.actuate.web.mappings.servlet.FilterRegistrationMappingDescription;
import org.springframework.boot.actuate.web.mappings.servlet.FiltersMappingDescriptionProvider;
import org.springframework.boot.actuate.web.mappings.servlet.ServletRegistrationMappingDescription;
import org.springframework.boot.actuate.web.mappings.servlet.ServletsMappingDescriptionProvider;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MappingsEndpoint}.
 *
 * @author Andy Wilkinson
 */
public class MappingsEndpointTests {

	@Test
	@SuppressWarnings("unchecked")
	public void servletWebMappings() {
		ServletContext servletContext = mock(ServletContext.class);
		given(servletContext.getInitParameterNames())
				.willReturn(Collections.emptyEnumeration());
		given(servletContext.getAttributeNames())
				.willReturn(Collections.emptyEnumeration());
		FilterRegistration filterRegistration = mock(FilterRegistration.class);
		given((Map<String, FilterRegistration>) servletContext.getFilterRegistrations())
				.willReturn(Collections.singletonMap("testFilter", filterRegistration));
		ServletRegistration servletRegistration = mock(ServletRegistration.class);
		given((Map<String, ServletRegistration>) servletContext.getServletRegistrations())
				.willReturn(Collections.singletonMap("testServlet", servletRegistration));
		Supplier<ConfigurableWebApplicationContext> contextSupplier = () -> {
			AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
			context.setServletContext(servletContext);
			return context;
		};
		new WebApplicationContextRunner(contextSupplier)
				.withUserConfiguration(EndpointConfiguration.class,
						ServletWebConfiguration.class)
				.run((context) -> {
					ContextMappings contextMappings = contextMappings(context);
					assertThat(contextMappings.getParentId()).isNull();
					assertThat(contextMappings.getMappings()).containsOnlyKeys(
							"dispatcherServlets", "servletFilters", "servlets");
					Map<String, List<DispatcherServletMappingDescription>> dispatcherServlets = mappings(
							contextMappings, "dispatcherServlets");
					assertThat(dispatcherServlets).containsOnlyKeys("dispatcherServlet");
					List<DispatcherServletMappingDescription> handlerMappings = dispatcherServlets
							.get("dispatcherServlet");
					assertThat(handlerMappings).hasSize(1);
					List<ServletRegistrationMappingDescription> servlets = mappings(
							contextMappings, "servlets");
					assertThat(servlets).hasSize(1);
					List<FilterRegistrationMappingDescription> filters = mappings(
							contextMappings, "servletFilters");
					assertThat(filters).hasSize(1);
				});
	}

	@Test
	public void reactiveWebMappings() {
		new ReactiveWebApplicationContextRunner()
				.withUserConfiguration(EndpointConfiguration.class,
						ReactiveWebConfiguration.class)
				.run((context) -> {
					ContextMappings contextMappings = contextMappings(context);
					assertThat(contextMappings.getParentId()).isNull();
					assertThat(contextMappings.getMappings())
							.containsOnlyKeys("dispatcherHandlers");
					Map<String, List<DispatcherHandlerMappingDescription>> dispatcherHandlers = mappings(
							contextMappings, "dispatcherHandlers");
					assertThat(dispatcherHandlers).containsOnlyKeys("webHandler");
					List<DispatcherHandlerMappingDescription> handlerMappings = dispatcherHandlers
							.get("webHandler");
					assertThat(handlerMappings).hasSize(3);
				});
	}

	private ContextMappings contextMappings(ApplicationContext context) {
		ApplicationMappings applicationMappings = context.getBean(MappingsEndpoint.class)
				.mappings();
		assertThat(applicationMappings.getContexts()).containsOnlyKeys(context.getId());
		return applicationMappings.getContexts().get(context.getId());
	}

	@SuppressWarnings("unchecked")
	private <T> T mappings(ContextMappings contextMappings, String key) {
		return (T) contextMappings.getMappings().get(key);
	}

	@Configuration
	static class EndpointConfiguration {

		@Bean
		public MappingsEndpoint mappingsEndpoint(
				Collection<MappingDescriptionProvider> descriptionProviders,
				ApplicationContext context) {
			return new MappingsEndpoint(descriptionProviders, context);
		}

	}

	@Configuration
	@EnableWebFlux
	@Controller
	static class ReactiveWebConfiguration {

		@Bean
		public DispatcherHandlersMappingDescriptionProvider dispatcherHandlersMappingDescriptionProvider() {
			return new DispatcherHandlersMappingDescriptionProvider();
		}

		@Bean
		public RouterFunction<ServerResponse> routerFunction() {
			return RouterFunctions
					.route(RequestPredicates.GET("/one"),
							(request) -> ServerResponse.ok().build())
					.andRoute(RequestPredicates.POST("/two"),
							(request) -> ServerResponse.ok().build());
		}

		@RequestMapping("/three")
		public void three() {

		}

	}

	@Configuration
	@EnableWebMvc
	@Controller
	static class ServletWebConfiguration {

		@Bean
		public DispatcherServletsMappingDescriptionProvider dispatcherServletsMappingDescriptionProvider() {
			return new DispatcherServletsMappingDescriptionProvider();
		}

		@Bean
		public ServletsMappingDescriptionProvider servletsMappingDescriptionProvider() {
			return new ServletsMappingDescriptionProvider();
		}

		@Bean
		public FiltersMappingDescriptionProvider filtersMappingDescriptionProvider() {
			return new FiltersMappingDescriptionProvider();
		}

		@Bean
		public DispatcherServlet dispatcherServlet(WebApplicationContext context)
				throws ServletException {
			DispatcherServlet dispatcherServlet = new DispatcherServlet(context);
			dispatcherServlet.init(new MockServletConfig());
			return dispatcherServlet;
		}

		@RequestMapping("/three")
		public void three() {

		}

	}

}
