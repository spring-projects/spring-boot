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

package smoketest.actuator.customsecurity;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.http.client.BaseUrlUriBuilderFactory;
import org.springframework.boot.test.http.server.BaseUrl;
import org.springframework.boot.test.http.server.BaseUrlProviders;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for cors preflight requests to management endpoints.
 *
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("cors")
@AutoConfigureTestRestTemplate
class CorsSampleActuatorApplicationTests {

	private TestRestTemplate testRestTemplate;

	@Autowired
	private ApplicationContext applicationContext;

	@BeforeEach
	void setUp() {
		RestTemplateBuilder builder = new RestTemplateBuilder();
		BaseUrl baseUrl = new BaseUrlProviders(this.applicationContext).getBaseUrlOrDefault();
		builder = builder.uriTemplateHandler(BaseUrlUriBuilderFactory.get(baseUrl));
		this.testRestTemplate = new TestRestTemplate(builder);
	}

	@Test
	void endpointShouldReturnUnauthorized() {
		ResponseEntity<?> entity = this.testRestTemplate.getForEntity("/actuator/env", Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void preflightRequestToEndpointShouldReturnOk() throws Exception {
		RequestEntity<?> envRequest = RequestEntity.options(new URI("/actuator/env"))
			.header("Origin", "http://localhost:8080")
			.header("Access-Control-Request-Method", "GET")
			.build();
		ResponseEntity<?> exchange = this.testRestTemplate.exchange(envRequest, Map.class);
		assertThat(exchange.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void preflightRequestWhenCorsConfigInvalidShouldReturnForbidden() throws Exception {
		RequestEntity<?> entity = RequestEntity.options(new URI("/actuator/env"))
			.header("Origin", "http://localhost:9095")
			.header("Access-Control-Request-Method", "GET")
			.build();
		ResponseEntity<byte[]> exchange = this.testRestTemplate.exchange(entity, byte[].class);
		assertThat(exchange.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

}
