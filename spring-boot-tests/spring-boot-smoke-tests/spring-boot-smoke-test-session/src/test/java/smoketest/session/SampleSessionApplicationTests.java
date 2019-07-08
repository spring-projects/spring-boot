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

package smoketest.session;

import java.net.URI;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleSessionApplication}.
 *
 * @author Andy Wilkinson
 * @author Vedran Pavic
 */
class SampleSessionApplicationTests {

	@Test
	void sessionExpiry() throws Exception {
		ConfigurableApplicationContext context = createContext();
		String port = context.getEnvironment().getProperty("local.server.port");
		URI uri = URI.create("http://localhost:" + port + "/");
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> firstResponse = firstRequest(restTemplate, uri);
		String sessionId1 = firstResponse.getBody();
		String cookie = firstResponse.getHeaders().getFirst("Set-Cookie");
		String sessionId2 = nextRequest(restTemplate, uri, cookie).getBody();
		assertThat(sessionId1).isEqualTo(sessionId2);
		Thread.sleep(1000);
		String loginPage = nextRequest(restTemplate, uri, cookie).getBody();
		assertThat(loginPage).containsIgnoringCase("login");
	}

	private ConfigurableApplicationContext createContext() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder().sources(SampleSessionApplication.class)
				.properties("server.port:0", "server.servlet.session.timeout:1")
				.initializers(new ServerPortInfoApplicationContextInitializer()).run();
		return context;
	}

	private ResponseEntity<String> firstRequest(RestTemplate restTemplate, URI uri) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", getBasicAuth());
		RequestEntity<Object> request = new RequestEntity<>(headers, HttpMethod.GET, uri);
		return restTemplate.exchange(request, String.class);
	}

	private ResponseEntity<String> nextRequest(RestTemplate restTemplate, URI uri, String cookie) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Cookie", cookie);
		RequestEntity<Object> request = new RequestEntity<>(headers, HttpMethod.GET, uri);
		return restTemplate.exchange(request, String.class);
	}

	private String getBasicAuth() {
		return "Basic " + Base64.getEncoder().encodeToString("user:password".getBytes());
	}

}
