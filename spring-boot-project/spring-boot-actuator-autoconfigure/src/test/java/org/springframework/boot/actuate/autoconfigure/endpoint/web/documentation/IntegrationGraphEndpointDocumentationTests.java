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

import org.springframework.boot.actuate.integration.IntegrationGraphEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.graph.IntegrationGraphServer;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for generating documentation describing the {@link IntegrationGraphEndpoint}.
 *
 * @author Tim Ysewyn
 */
class IntegrationGraphEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	@Test
	void graph() {
		assertThat(this.mvc.get().uri("/actuator/integrationgraph")).hasStatusOk()
			.apply(MockMvcRestDocumentation.document("integrationgraph/graph"));
	}

	@Test
	void rebuild() {
		assertThat(this.mvc.post().uri("/actuator/integrationgraph")).hasStatus(HttpStatus.NO_CONTENT)
			.apply(MockMvcRestDocumentation.document("integrationgraph/rebuild"));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		IntegrationGraphServer integrationGraphServer() {
			return new IntegrationGraphServer();
		}

		@Bean
		IntegrationGraphEndpoint endpoint(IntegrationGraphServer integrationGraphServer) {
			return new IntegrationGraphEndpoint(integrationGraphServer);
		}

	}

}
