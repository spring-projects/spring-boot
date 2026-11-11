/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.docs.context.properties;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint;
import org.springframework.boot.actuate.docs.MockMvcEndpointDocumentationTests;
import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;

/**
 * Tests for generating documentation describing
 * {@link ConfigurationPropertiesReportEndpoint}.
 *
 * @author Andy Wilkinson
 * @author Chris Bono
 */
class ConfigurationPropertiesReportEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	@Test
	void configProps() {
		assertThat(this.mvc.get().uri("/actuator/configprops")).hasStatusOk()
			.apply(MockMvcRestDocumentation.document("configprops/all",
					preprocessResponse(limit("contexts", getApplicationContext().getId(), "beans")),
					responseFields(fieldWithPath("contexts").description("Application contexts keyed by id."),
							fieldWithPath("contexts.*.beans.*")
								.description("`@ConfigurationProperties` beans keyed by bean name."),
							fieldWithPath("contexts.*.beans.*.prefix")
								.description("Prefix applied to the names of the bean's properties."),
							subsectionWithPath("contexts.*.beans.*.properties")
								.description("Properties of the bean as name-value pairs."),
							subsectionWithPath("contexts.*.beans.*.inputs").description(
									"Origin and value of the configuration property used when binding to this bean."),
							parentIdField())));
	}

	@Test
	void configPropsFilterByPrefix() {
		assertThat(this.mvc.get().uri("/actuator/configprops/spring.jackson")).hasStatusOk()
			.apply(MockMvcRestDocumentation.document("configprops/prefixed",
					preprocessResponse(limit("contexts", getApplicationContext().getId(), "beans")),
					responseFields(fieldWithPath("contexts").description("Application contexts keyed by id."),
							fieldWithPath("contexts.*.beans.*")
								.description("`@ConfigurationProperties` beans keyed by bean name."),
							fieldWithPath("contexts.*.beans.*.prefix")
								.description("Prefix applied to the names of the bean's properties."),
							subsectionWithPath("contexts.*.beans.*.properties")
								.description("Properties of the bean as name-value pairs."),
							subsectionWithPath("contexts.*.beans.*.inputs").description(
									"Origin and value of the configuration property used when binding to this bean."),
							parentIdField())));
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		ConfigurationPropertiesReportEndpoint endpoint() {
			return new ConfigurationPropertiesReportEndpoint(Collections.emptyList(), Show.ALWAYS);
		}

	}

}
