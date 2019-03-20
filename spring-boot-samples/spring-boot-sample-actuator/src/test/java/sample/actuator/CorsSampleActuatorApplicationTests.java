/*
 * Copyright 2012-2018 the original author or authors.
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

package sample.actuator;

import java.net.URI;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for cors preflight requests to management endpoints.
 *
 * @author Madhura Bhave
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@ActiveProfiles("cors")
public class CorsSampleActuatorApplicationTests {

	private TestRestTemplate testRestTemplate;

	@Autowired
	ApplicationContext applicationContext;

	@Before
	public void setUp() throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		LocalHostUriTemplateHandler handler = new LocalHostUriTemplateHandler(
				this.applicationContext.getEnvironment(), "http");
		restTemplate.setUriTemplateHandler(handler);
		restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
		this.testRestTemplate = new TestRestTemplate(restTemplate);
	}

	@Test
	public void sensitiveEndpointShouldReturnUnauthorized() throws Exception {
		ResponseEntity<?> entity = this.testRestTemplate.getForEntity("/env", Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void preflightRequestForInsensitiveShouldReturnOk() throws Exception {
		RequestEntity<?> healthRequest = RequestEntity.options(new URI("/health"))
				.header("Origin", "http://localhost:8080")
				.header("Access-Control-Request-Method", "GET").build();
		ResponseEntity<?> exchange = this.testRestTemplate.exchange(healthRequest,
				Map.class);
		assertThat(exchange.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void preflightRequestForSensitiveEndpointShouldReturnOk() throws Exception {
		RequestEntity<?> entity = RequestEntity.options(new URI("/env"))
				.header("Origin", "http://localhost:8080")
				.header("Access-Control-Request-Method", "GET").build();
		ResponseEntity<?> env = this.testRestTemplate.exchange(entity, Map.class);
		assertThat(env.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void preflightRequestWhenCorsConfigInvalidShouldReturnForbidden()
			throws Exception {
		RequestEntity<?> entity = RequestEntity.options(new URI("/health"))
				.header("Origin", "http://localhost:9095")
				.header("Access-Control-Request-Method", "GET").build();
		ResponseEntity<byte[]> exchange = this.testRestTemplate.exchange(entity,
				byte[].class);
		assertThat(exchange.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

}
