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
import org.springframework.boot.actuate.web.mappings.servlet.DispatcherServletsMappingDescriptionProvider;
import org.springframework.boot.actuate.web.mappings.servlet.FiltersMappingDescriptionProvider;
import org.springframework.boot.actuate.web.mappings.servlet.ServletsMappingDescriptionProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.restdocs.payload.PayloadDocumentation.beneathPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;

/**
 * Tests for generating documentation describing {@link MappingsEndpoint}.
 *
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class MappingsEndpointServletDocumentationTests
		extends AbstractEndpointDocumentationTests {

	@Rule
	public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

	@LocalServerPort
	private int port;

	private WebTestClient client;

	@Before
	public void webTestClient() {
		this.client = WebTestClient.bindToServer()
				.filter(documentationConfiguration(this.restDocumentation))
				.baseUrl("http://localhost:" + this.port).build();
	}

	@Test
	public void mappings() throws Exception {
		ResponseFieldsSnippet commonResponseFields = responseFields(
				fieldWithPath("contexts")
						.description("Application contexts keyed by id."),
				fieldWithPath("contexts.*.mappings")
						.description("Mappings in the context, keyed by mapping type."),
				subsectionWithPath("contexts.*.mappings.dispatcherServlets")
						.description("Dispatcher servlet mappings, if any."),
				subsectionWithPath("contexts.*.mappings.servletFilters")
						.description("Servlet filter mappings, if any."),
				subsectionWithPath("contexts.*.mappings.servlets")
						.description("Servlet mappings, if any."),
				subsectionWithPath("contexts.*.mappings.dispatcherHandlers")
						.description("Dispatcher handler mappings, if any.").optional()
						.type(JsonFieldType.OBJECT),
				parentIdField());
		this.client.get().uri("/actuator/mappings").exchange().expectBody()
				.consumeWith(document("mappings", commonResponseFields,
						responseFields(
								beneathPath("contexts.*.mappings.dispatcherServlets")
										.withSubsectionId("dispatcher-servlets"),
						fieldWithPath("*").description(
								"Dispatcher servlet mappings, if any, keyed by "
										+ "dispatcher servlet bean name."),
						fieldWithPath("*.[].handler")
								.description("Handler for the mapping."),
						fieldWithPath("*.[].predicate")
								.description("Predicate for the mapping.")),
						responseFields(
								beneathPath("contexts.*.mappings.servletFilters")
										.withSubsectionId("servlet-filters"),
								fieldWithPath("[].servletNameMappings").description(
										"Names of the servlets to which the filter is mapped."),
								fieldWithPath("[].urlPatternMappings").description(
										"URL pattern to which the filter is mapped."),
								fieldWithPath("[].name")
										.description("Name of the filter."),
								fieldWithPath("[].className")
										.description("Class name of the filter")),
						responseFields(
								beneathPath("contexts.*.mappings.servlets")
										.withSubsectionId("servlets"),
								fieldWithPath("[].mappings")
										.description("Mappings of the servlet."),
								fieldWithPath("[].name")
										.description("Name of the servlet."),
								fieldWithPath("[].className")
										.description("Class name of the servlet"))));
	}

	@Configuration
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		public TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

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
		public MappingsEndpoint mappingsEndpoint(
				Collection<MappingDescriptionProvider> descriptionProviders,
				ConfigurableApplicationContext context) {
			return new MappingsEndpoint(descriptionProviders, context);
		}

	}

}
