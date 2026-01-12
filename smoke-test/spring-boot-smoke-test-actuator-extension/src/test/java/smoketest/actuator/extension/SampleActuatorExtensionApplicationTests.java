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

package smoketest.actuator.extension;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "spring.web.error.include-message=always" })
@AutoConfigureTestRestTemplate
class SampleActuatorExtensionApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private RestTemplateBuilder restTemplateBuilder;

	@Test
	@SuppressWarnings("rawtypes")
	void healthActuatorIsNotExposed() {
		ResponseEntity<Map> entity = this.restTemplate.getForEntity("/actuator/health", Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@SuppressWarnings("rawtypes")
	void healthExtensionWithAuthHeaderIsDenied() {
		ResponseEntity<Map> entity = this.restTemplate.getForEntity("/myextension/health", Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@SuppressWarnings("rawtypes")
	void healthExtensionWithAuthHeader() {
		TestRestTemplate restTemplate = new TestRestTemplate(
				this.restTemplateBuilder.defaultHeader("Authorization", "Bearer secret"));
		LocalTestWebServer localTestWebServer = LocalTestWebServer.obtain(this.applicationContext);
		restTemplate.setUriTemplateHandler(localTestWebServer.uriBuilderFactory());
		ResponseEntity<Map> entity = restTemplate.getForEntity("/myextension/health", Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

}
