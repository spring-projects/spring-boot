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

package org.springframework.boot.webflux.actuate.web.mappings;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.webflux.actuate.web.mappings.DispatcherHandlersMappingDescriptionProvider.DispatcherHandlersMappingDescriptionProviderRuntimeHints;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

/**
 * Tests for {@link DispatcherHandlersMappingDescriptionProvider}.
 *
 * @author Moritz Halbritter
 * @author Brian Clozel
 */
class DispatcherHandlersMappingDescriptionProviderTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new DispatcherHandlersMappingDescriptionProviderRuntimeHints().registerHints(runtimeHints,
				getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(DispatcherHandlerMappingDescription.class)
			.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(runtimeHints);
	}

	@Test
	void shouldDescribeAnnotatedControllers() {
		new ReactiveWebApplicationContextRunner().withUserConfiguration(ControllerWebConfiguration.class)
			.run((context) -> {

				Map<String, List<DispatcherHandlerMappingDescription>> describedMappings = new DispatcherHandlersMappingDescriptionProvider()
					.describeMappings(context);
				assertThat(describedMappings).hasSize(1).containsOnlyKeys("webHandler");
				List<DispatcherHandlerMappingDescription> descriptions = describedMappings.get("webHandler");
				assertThat(descriptions).hasSize(2)
					.extracting("predicate")
					.containsExactlyInAnyOrder("{POST /api/projects, consumes [application/json]}",
							"{GET /api/projects/{id}, produces [application/json]}");
			});
	}

	@Test
	void shouldDescribeRouterFunctions() {
		new ReactiveWebApplicationContextRunner().withUserConfiguration(RouterConfiguration.class).run((context) -> {

			Map<String, List<DispatcherHandlerMappingDescription>> describedMappings = new DispatcherHandlersMappingDescriptionProvider()
				.describeMappings(context);
			assertThat(describedMappings).hasSize(1).containsOnlyKeys("webHandler");
			List<DispatcherHandlerMappingDescription> descriptions = describedMappings.get("webHandler");
			assertThat(descriptions).hasSize(2)
				.extracting("predicate")
				.containsExactlyInAnyOrder("(/api && (POST && (/projects/ && Content-Type: application/json)))",
						"(/api && (GET && (/projects//{id} && Accept: application/json)))");
		});
	}

	@SuppressWarnings("unchecked")
	@Configuration(proxyBeanMethods = false)
	@EnableWebFlux
	static class ControllerWebConfiguration {

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
	@EnableWebFlux
	static class RouterConfiguration {

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
