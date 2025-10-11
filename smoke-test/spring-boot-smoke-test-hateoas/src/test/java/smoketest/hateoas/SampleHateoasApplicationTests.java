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

package smoketest.hateoas;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SampleHateoasApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void hasHalLinksWhenAnythingIsAcceptable() {
		HttpHeaders headers = new HttpHeaders();
		ResponseEntity<String> entity = this.restTemplate.exchange("/customers/1", HttpMethod.GET,
				new HttpEntity<>(headers), String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).startsWith("{\"_links\":{\"self\":{\"href\"");
		assertThat(entity.getBody()).endsWith(",\"id\":1,\"firstName\":\"Oliver\",\"lastName\":\"Gierke\"}");
	}

	@Test
	void hasHalLinksWhenJsonIsAcceptable() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		ResponseEntity<String> entity = this.restTemplate.exchange("/customers/1", HttpMethod.GET,
				new HttpEntity<>(headers), String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).startsWith("{\"_links\":{\"self\":{\"href\"");
		assertThat(entity.getBody()).endsWith(",\"id\":1,\"firstName\":\"Oliver\",\"lastName\":\"Gierke\"}");
	}

	@Test
	void producesJsonWhenXmlIsPreferred() {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.ACCEPT, "application/xml;q=0.9,application/json;q=0.8");
		HttpEntity<?> request = new HttpEntity<>(headers);
		ResponseEntity<String> response = this.restTemplate.exchange("/customers/1", HttpMethod.GET, request,
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("application/json"));
	}

}
