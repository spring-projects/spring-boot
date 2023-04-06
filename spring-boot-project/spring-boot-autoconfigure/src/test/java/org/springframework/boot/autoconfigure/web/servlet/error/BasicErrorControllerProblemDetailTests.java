/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet.error;

import java.io.IOException;
import java.net.URI;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BasicErrorController} producing
 * {@link org.springframework.http.ProblemDetail}
 *
 * @author Yanming Zhou
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "server.error.include-message=always", "spring.mvc.problemdetails.enabled=true" })
class BasicErrorControllerProblemDetailTests {

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Test
	void sendErrorShouldProduceProblemDetails() {
		String path = "/conflict";
		ResponseEntity<ProblemDetail> resp = this.testRestTemplate.exchange(RequestEntity.method(HttpMethod.GET, path)
			.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.build(), ProblemDetail.class);
		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		ProblemDetail expected = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Conflicts arise");
		expected.setTitle(HttpStatus.CONFLICT.getReasonPhrase());
		expected.setInstance(URI.create(path));
		assertThat(resp.getBody()).isEqualTo(expected);
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ ServletWebServerFactoryAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
			WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			ErrorMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	static class TestConfiguration {

		@RestController
		public static class TestController {

			@RequestMapping("/conflict")
			void home(HttpServletResponse response) throws IOException {
				response.sendError(HttpServletResponse.SC_CONFLICT, "Conflicts arise");
			}

		}

	}

}
