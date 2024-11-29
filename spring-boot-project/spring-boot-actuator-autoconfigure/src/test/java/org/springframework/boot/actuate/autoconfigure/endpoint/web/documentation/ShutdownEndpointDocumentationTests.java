/*
 * Copyright 2012-2024 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.context.ShutdownEndpoint;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		ShutdownEndpoint endpoint() {
			ShutdownEndpoint endpoint = new ShutdownEndpoint();
			endpoint.setApplicationContext(new AnnotationConfigApplicationContext());
			return endpoint;
		}

	}

}
