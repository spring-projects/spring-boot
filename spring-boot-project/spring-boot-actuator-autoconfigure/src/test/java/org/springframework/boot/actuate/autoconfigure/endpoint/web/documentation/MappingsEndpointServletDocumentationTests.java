/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

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
@ExtendWith(RestDocumentationExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MappingsEndpointServletDocumentationTests extends AbstractEndpointDocumentationTests {

	@LocalServerPort
	private int port;

	private WebTestClient client;

	@BeforeEach
	void webTestClient(RestDocumentationContextProvider restDocumentation) {
		this.client = WebTestClient.bindToServer().filter(documentationConfiguration(restDocumentation))
				.baseUrl("http://localhost:" + this.port).build();
	}

	@Test
	void mappings() {
		ResponseFieldsSnippet commonResponseFields = responseFields(
				fieldWithPath("contexts").description("Application contexts keyed by id."),
				fieldWithPath("contexts.*.mappings").description("Mappings in the context, keyed by mapping type."),
				subsectionWithPath("contexts.*.mappings.dispatcherServlets")
						.description("Dispatcher servlet mappings, if any."),
				subsectionWithPath("contexts.*.mappings.servletFilters")
						.description("Servlet filter mappings, if any."),
				subsectionWithPath("contexts.*.mappings.servlets").description("Servlet mappings, if any."),
				subsectionWithPath("contexts.*.mappings.dispatcherHandlers")
						.description("Dispatcher handler mappings, if any.").optional().type(JsonFieldType.OBJECT),
				parentIdField());
		List<FieldDescriptor> dispatcherServletFields = new ArrayList<>(Arrays.asList(
				fieldWithPath("*")
						.description("Dispatcher servlet mappings, if any, keyed by dispatcher servlet bean name."),
				fieldWithPath("*.[].details").optional().type(JsonFieldType.OBJECT)
						.description("Additional implementation-specific details about the mapping. Optional."),
				fieldWithPath("*.[].handler").description("Handler for the mapping."),
				fieldWithPath("*.[].predicate").description("Predicate for the mapping.")));
		List<FieldDescriptor> requestMappingConditions = Arrays.asList(
				requestMappingConditionField("").description("Details of the request mapping conditions.").optional(),
				requestMappingConditionField(".consumes").description("Details of the consumes condition"),
				requestMappingConditionField(".consumes.[].mediaType").description("Consumed media type."),
				requestMappingConditionField(".consumes.[].negated").description("Whether the media type is negated."),
				requestMappingConditionField(".headers").description("Details of the headers condition."),
				requestMappingConditionField(".headers.[].name").description("Name of the header."),
				requestMappingConditionField(".headers.[].value").description("Required value of the header, if any."),
				requestMappingConditionField(".headers.[].negated").description("Whether the value is negated."),
				requestMappingConditionField(".methods").description("HTTP methods that are handled."),
				requestMappingConditionField(".params").description("Details of the params condition."),
				requestMappingConditionField(".params.[].name").description("Name of the parameter."),
				requestMappingConditionField(".params.[].value")
						.description("Required value of the parameter, if any."),
				requestMappingConditionField(".params.[].negated").description("Whether the value is negated."),
				requestMappingConditionField(".patterns")
						.description("Patterns identifying the paths handled by the mapping."),
				requestMappingConditionField(".produces").description("Details of the produces condition."),
				requestMappingConditionField(".produces.[].mediaType").description("Produced media type."),
				requestMappingConditionField(".produces.[].negated").description("Whether the media type is negated."));
		List<FieldDescriptor> handlerMethod = Arrays.asList(
				fieldWithPath("*.[].details.handlerMethod").optional().type(JsonFieldType.OBJECT)
						.description("Details of the method, if any, that will handle requests to this mapping."),
				fieldWithPath("*.[].details.handlerMethod.className")
						.description("Fully qualified name of the class of the method."),
				fieldWithPath("*.[].details.handlerMethod.name").description("Name of the method."),
				fieldWithPath("*.[].details.handlerMethod.descriptor")
						.description("Descriptor of the method as specified in the Java Language Specification."));
		dispatcherServletFields.addAll(handlerMethod);
		dispatcherServletFields.addAll(requestMappingConditions);
		this.client.get().uri("/actuator/mappings").exchange().expectBody()
				.consumeWith(document("mappings", commonResponseFields,
						responseFields(beneathPath("contexts.*.mappings.dispatcherServlets")
								.withSubsectionId("dispatcher-servlets"), dispatcherServletFields),
						responseFields(
								beneathPath("contexts.*.mappings.servletFilters").withSubsectionId("servlet-filters"),
								fieldWithPath("[].servletNameMappings")
										.description("Names of the servlets to which the filter is mapped."),
								fieldWithPath("[].urlPatternMappings")
										.description("URL pattern to which the filter is mapped."),
								fieldWithPath("[].name").description("Name of the filter."),
								fieldWithPath("[].className").description("Class name of the filter")),
						responseFields(beneathPath("contexts.*.mappings.servlets").withSubsectionId("servlets"),
								fieldWithPath("[].mappings").description("Mappings of the servlet."),
								fieldWithPath("[].name").description("Name of the servlet."),
								fieldWithPath("[].className").description("Class name of the servlet"))));
	}

	private FieldDescriptor requestMappingConditionField(String path) {
		return fieldWithPath("*.[].details.requestMappingConditions" + path);
	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		DispatcherServletsMappingDescriptionProvider dispatcherServletsMappingDescriptionProvider() {
			return new DispatcherServletsMappingDescriptionProvider();
		}

		@Bean
		ServletsMappingDescriptionProvider servletsMappingDescriptionProvider() {
			return new ServletsMappingDescriptionProvider();
		}

		@Bean
		FiltersMappingDescriptionProvider filtersMappingDescriptionProvider() {
			return new FiltersMappingDescriptionProvider();
		}

		@Bean
		MappingsEndpoint mappingsEndpoint(Collection<MappingDescriptionProvider> descriptionProviders,
				ConfigurableApplicationContext context) {
			return new MappingsEndpoint(descriptionProviders, context);
		}

		@Bean
		ExampleController exampleController() {
			return new ExampleController();
		}

	}

	@RestController
	static class ExampleController {

		@PostMapping(path = "/", consumes = { MediaType.APPLICATION_JSON_VALUE, "!application/xml" },
				produces = MediaType.TEXT_PLAIN_VALUE, headers = "X-Custom=Foo", params = "a!=alpha")
		String example() {
			return "Hello World";
		}

	}

}
