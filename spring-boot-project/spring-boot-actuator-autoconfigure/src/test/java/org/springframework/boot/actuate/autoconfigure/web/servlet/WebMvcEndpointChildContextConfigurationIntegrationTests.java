/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.servlet;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link WebMvcEndpointChildContextConfiguration}.
 *
 * @author Phillip Webb
 */
class WebMvcEndpointChildContextConfigurationIntegrationTests {

	@Test // gh-17938
	void errorPageAndErrorControllerAreUsed() {
		new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class,
						ServletWebServerFactoryAutoConfiguration.class, ServletManagementContextAutoConfiguration.class,
						WebEndpointAutoConfiguration.class, EndpointAutoConfiguration.class,
						DispatcherServletAutoConfiguration.class, ErrorMvcAutoConfiguration.class))
				.withUserConfiguration(FailingEndpoint.class)
				.withInitializer(new ServerPortInfoApplicationContextInitializer()).withPropertyValues("server.port=0",
						"management.server.port=0", "management.endpoints.web.exposure.include=*")
				.run((context) -> {
					String port = context.getEnvironment().getProperty("local.management.port");
					WebClient client = WebClient.create("http://localhost:" + port);
					ClientResponse response = client.get().uri("actuator/fail").accept(MediaType.APPLICATION_JSON)
							.exchange().block();
					assertThat(response.bodyToMono(String.class).block()).contains("message\":\"Epic Fail");
				});
	}

	@Component
	@Endpoint(id = "fail")
	static class FailingEndpoint {

		@ReadOperation
		String fail() {
			throw new IllegalStateException("Epic Fail");
		}

	}

}
