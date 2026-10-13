/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.docs.context;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.context.ShutdownEndpoint;
import org.springframework.boot.actuate.docs.MockMvcEndpointDocumentationTests;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

/**
 * Tests for generating documentation describing the {@link ShutdownEndpoint}.
 *
 * @author Andy Wilkinson
 */
@TestPropertySource(properties = "management.endpoint.shutdown.access=unrestricted")
class ShutdownEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	@Test
	void shutdown() {
		assertThat(this.mvc.post().uri("/actuator/shutdown")).hasStatusOk()
			.apply(MockMvcRestDocumentation.document("shutdown", responseFields(
					fieldWithPath("message").description("Message describing the result of the request."))));
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		ShutdownEndpoint endpoint() {
			ShutdownEndpoint endpoint = new ShutdownEndpoint();
			endpoint.setApplicationContext(new AnnotationConfigApplicationContext());
			return endpoint;
		}

	}

}
