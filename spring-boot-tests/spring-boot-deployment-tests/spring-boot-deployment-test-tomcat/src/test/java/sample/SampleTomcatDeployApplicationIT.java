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

package sample;

import java.net.URI;

import org.junit.Test;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for {@link SampleTomcatDeployApplication}.
 */
public class SampleTomcatDeployApplicationIT {

	private final TestRestTemplate rest = new TestRestTemplate();

	private int port = Integer.valueOf(System.getProperty("port"));

	@Test
	public void testHome() throws Exception {
		String url = "http://localhost:" + this.port + "/bootapp/";
		ResponseEntity<String> entity = this.rest.getForEntity(url, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Hello World");
	}

	@Test
	public void testHealth() throws Exception {
		String url = "http://localhost:" + this.port + "/bootapp/actuator/health";
		System.out.println(url);
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(url, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("{\"status\":\"UP\"}");
	}

	@Test
	public void errorFromExceptionForRequestAcceptingAnythingProducesAJsonResponse() throws Exception {
		assertThatPathProducesExpectedResponse("/bootapp/exception", MediaType.ALL, MediaType.APPLICATION_JSON);
	}

	@Test
	public void errorFromExceptionForRequestAcceptingJsonProducesAJsonResponse() throws Exception {
		assertThatPathProducesExpectedResponse("/bootapp/exception", MediaType.APPLICATION_JSON,
				MediaType.APPLICATION_JSON);
	}

	@Test
	public void errorFromExceptionForRequestAcceptingHtmlProducesAnHtmlResponse() throws Exception {
		assertThatPathProducesExpectedResponse("/bootapp/exception", MediaType.TEXT_HTML, MediaType.TEXT_HTML);
	}

	@Test
	public void sendErrorForRequestAcceptingAnythingProducesAJsonResponse() throws Exception {
		assertThatPathProducesExpectedResponse("/bootapp/send-error", MediaType.ALL, MediaType.APPLICATION_JSON);
	}

	@Test
	public void sendErrorForRequestAcceptingJsonProducesAJsonResponse() throws Exception {
		assertThatPathProducesExpectedResponse("/bootapp/send-error", MediaType.APPLICATION_JSON,
				MediaType.APPLICATION_JSON);
	}

	@Test
	public void sendErrorForRequestAcceptingHtmlProducesAnHtmlResponse() throws Exception {
		assertThatPathProducesExpectedResponse("/bootapp/send-error", MediaType.TEXT_HTML, MediaType.TEXT_HTML);
	}

	private void assertThatPathProducesExpectedResponse(String path, MediaType accept, MediaType contentType) {
		RequestEntity<Void> request = RequestEntity.get(URI.create("http://localhost:" + this.port + path))
				.accept(accept).build();
		ResponseEntity<String> response = this.rest.exchange(request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(contentType.isCompatibleWith(response.getHeaders().getContentType()))
				.as("%s is compatible with %s", contentType, response.getHeaders().getContentType()).isTrue();
	}

}
