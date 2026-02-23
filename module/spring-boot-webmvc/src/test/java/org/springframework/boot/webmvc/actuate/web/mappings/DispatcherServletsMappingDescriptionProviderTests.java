/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.webmvc.actuate.web.mappings;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.boot.webmvc.actuate.web.mappings.DispatcherServletsMappingDescriptionProvider.DispatcherServletsMappingDescriptionProviderRuntimeHints;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.web.servlet.function.RequestPredicates.accept;
import static org.springframework.web.servlet.function.RequestPredicates.contentType;
import static org.springframework.web.servlet.function.RequestPredicates.path;

/**
 * Tests for {@link DispatcherServletsMappingDescriptionProvider}.
 *
 * @author Moritz Halbritter
 * @author Brian Clozel
 */
class DispatcherServletsMappingDescriptionProviderTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new DispatcherServletsMappingDescriptionProviderRuntimeHints().registerHints(runtimeHints,
				getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(DispatcherServletMappingDescription.class)
			.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(runtimeHints);
	}

	@Test
	void shouldDescribeAnnotatedControllers() {
		Supplier<ConfigurableWebApplicationContext> contextSupplier = prepareContextSupplier();
		new WebApplicationContextRunner(contextSupplier).withUserConfiguration(ControllerWebConfiguration.class)
			.run((context) -> {

				Map<String, List<DispatcherServletMappingDescription>> describedMappings = new DispatcherServletsMappingDescriptionProvider()
					.describeMappings(context);
				assertThat(describedMappings).hasSize(1).containsOnlyKeys("dispatcherServlet");
				List<DispatcherServletMappingDescription> descriptions = describedMappings.get("dispatcherServlet");
				assertThat(descriptions).hasSize(2)
					.extracting("predicate")
					.containsExactlyInAnyOrder("{POST [/api/projects], consumes [application/json]}",
							"{GET [/api/projects/{id}], produces [application/json]}");
			});
	}

	@Test
	void shouldDescribeRouterFunctions() {
		Supplier<ConfigurableWebApplicationContext> contextSupplier = prepareContextSupplier();
		new WebApplicationContextRunner(contextSupplier).withUserConfiguration(RouterConfiguration.class)
			.run((context) -> {

				Map<String, List<DispatcherServletMappingDescription>> describedMappings = new DispatcherServletsMappingDescriptionProvider()
					.describeMappings(context);
				assertThat(describedMappings).hasSize(1).containsOnlyKeys("dispatcherServlet");
				List<DispatcherServletMappingDescription> descriptions = describedMappings.get("dispatcherServlet");
				assertThat(descriptions).hasSize(2)
					.extracting("predicate")
					.containsExactlyInAnyOrder("(/api && (POST && (/projects/ && Content-Type: application/json)))",
							"(/api && (GET && (/projects//{id} && Accept: application/json)))");
			});
	}

	@SuppressWarnings("unchecked")
	private Supplier<ConfigurableWebApplicationContext> prepareContextSupplier() {
		ServletContext servletContext = mock(ServletContext.class);
		given(servletContext.getInitParameterNames()).willReturn(Collections.emptyEnumeration());
		given(servletContext.getAttributeNames()).willReturn(Collections.emptyEnumeration());
		FilterRegistration filterRegistration = mock(FilterRegistration.class);
		given((Map<String, FilterRegistration>) servletContext.getFilterRegistrations())
			.willReturn(Collections.singletonMap("testFilter", filterRegistration));
		ServletRegistration servletRegistration = mock(ServletRegistration.class);
		given((Map<String, ServletRegistration>) servletContext.getServletRegistrations())
			.willReturn(Collections.singletonMap("testServlet", servletRegistration));
		return () -> {
			AnnotationConfigServletWebApplicationContext context = new AnnotationConfigServletWebApplicationContext();
			context.setServletContext(servletContext);
			return context;
		};
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	static class ControllerWebConfiguration {

		@Bean
		DispatcherServlet dispatcherServlet(WebApplicationContext context) throws ServletException {
			DispatcherServlet dispatcherServlet = new DispatcherServlet(context);
			dispatcherServlet.init(new MockServletConfig());
			return dispatcherServlet;
		}

		@Controller
		@RequestMapping("/api")
		static class SampleController {

			@PostMapping(path = "/projects", consumes = MediaType.APPLICATION_JSON_VALUE)
			void createProject() {
			}

			@GetMapping(path = "/projects/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
			void findProject() {
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	static class RouterConfiguration {

		@Bean
		DispatcherServlet dispatcherServlet(WebApplicationContext context) throws ServletException {
			DispatcherServlet dispatcherServlet = new DispatcherServlet(context);
			dispatcherServlet.init(new MockServletConfig());
			return dispatcherServlet;
		}

		@Bean
		RouterFunction<ServerResponse> routerFunctions() {
			return RouterFunctions.route()
				.nest(path("/api"),
						(builder) -> builder
							.POST(path("/projects/").and(contentType(MediaType.APPLICATION_JSON)),
									(request) -> ServerResponse.ok().build())
							.GET(path("/projects//{id}").and(accept(MediaType.APPLICATION_JSON)),
									(request) -> ServerResponse.ok().build()))
				.build();
		}

	}

}
