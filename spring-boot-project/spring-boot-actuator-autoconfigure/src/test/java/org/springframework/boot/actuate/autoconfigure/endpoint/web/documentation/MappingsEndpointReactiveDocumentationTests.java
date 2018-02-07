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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import java.util.Collection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.web.mappings.MappingDescriptionProvider;
import org.springframework.boot.actuate.web.mappings.MappingsEndpoint;
import org.springframework.boot.actuate.web.mappings.reactive.DispatcherHandlersMappingDescriptionProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.restdocs.payload.PayloadDocumentation.beneathPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;

/**
 * Tests for generating documentation describing {@link MappingsEndpoint}.
 *
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.main.web-application-type=reactive")
public class MappingsEndpointReactiveDocumentationTests
		extends AbstractEndpointDocumentationTests {

	@Rule
	public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

	@LocalServerPort
	private int port;

	private WebTestClient client;

	@Before
	public void webTestClient() {
		this.client = WebTestClient
				.bindToServer().filter(documentationConfiguration(this.restDocumentation)
						.snippets().withDefaults())
				.baseUrl("http://localhost:" + this.port).build();
	}

	@Test
	public void mappings() throws Exception {
		this.client.get().uri("/actuator/mappings").exchange().expectStatus().isOk()
				.expectBody()
				.consumeWith(document("mappings",
						responseFields(
								beneathPath("contexts.*.mappings.dispatcherHandlers")
										.withSubsectionId("dispatcher-handlers"),
								fieldWithPath("*").description(
										"Dispatcher handler mappings, if any, keyed by "
												+ "dispatcher handler bean name."),
								fieldWithPath("*.[].handler")
										.description("Handler for the mapping."),
								fieldWithPath("*.[].predicate")
										.description("Predicate for the mapping."),
								fieldWithPath("*.[].details").optional()
										.type(JsonFieldType.OBJECT)
										.description("Additional implementation-specific "
												+ "details about the mapping. Optional."),
								fieldWithPath("*.[].details.handlerMethod").optional()
										.type(JsonFieldType.OBJECT)
										.description("Details of the method, if any, "
												+ "that will handle requests to "
												+ "this mapping."),
								fieldWithPath("*.[].details.handlerMethod.className")
										.type(JsonFieldType.STRING)
										.description("Fully qualified name of the class"
												+ " of the method."),
								fieldWithPath("*.[].details.handlerMethod.name")
										.type(JsonFieldType.STRING)
										.description("Name of the method."),
								fieldWithPath("*.[].details.handlerMethod.descriptor")
										.type(JsonFieldType.STRING)
										.description("Descriptor of the method as "
												+ "specified in the Java Language "
												+ "Specification."),
								fieldWithPath("*.[].details.handlerFunction")
										.optional().type(JsonFieldType.OBJECT)
										.description("Details of the function, if any, "
												+ "that will handle requests to this "
												+ "mapping."),
								fieldWithPath("*.[].details.handlerFunction.className")
										.type(JsonFieldType.STRING).description(
												"Fully qualified name of the class of "
														+ "the function."))));
	}

	@Configuration
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		public NettyReactiveWebServerFactory netty() {
			return new NettyReactiveWebServerFactory(0);
		}

		@Bean
		public DispatcherHandlersMappingDescriptionProvider dispatcherHandlersMappingDescriptionProvider() {
			return new DispatcherHandlersMappingDescriptionProvider();
		}

		@Bean
		public MappingsEndpoint mappingsEndpoint(
				Collection<MappingDescriptionProvider> descriptionProviders,
				ConfigurableApplicationContext context) {
			return new MappingsEndpoint(descriptionProviders, context);
		}

		@Bean
		public RouterFunction<ServerResponse> exampleRouter() {
			return RouterFunctions.route(RequestPredicates.GET("/foo"),
					(request) -> ServerResponse.ok().build());
		}

	}

}
