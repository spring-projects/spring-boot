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

package smoketest.jetty;

import org.junit.jupiter.api.Test;
import smoketest.jetty.util.StringUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Florian Storz
 * @author Michael Weidmann
 * @author Moritz Halbritter
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SampleJettyApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Value("${server.max-http-request-header-size}")
	private int maxHttpRequestHeaderSize;

	@Test
	void testHome() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Hello World");
	}

	@Test
	void testCompression() {
		// Jetty HttpClient sends Accept-Encoding: gzip by default
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Hello World");
		// Jetty HttpClient decodes gzip responses automatically and removes the
		// Content-Encoding header. We have to assume that the response was gzipped.
	}

	@Test
	void testMaxHttpResponseHeaderSize() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/max-http-response-header", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	void testMaxHttpRequestHeaderSize() {
		String headerValue = StringUtil.repeat('A', this.maxHttpRequestHeaderSize + 1);
		HttpHeaders headers = new HttpHeaders();
		headers.add("x-max-request-header", headerValue);
		HttpEntity<?> httpEntity = new HttpEntity<>(headers);
		ResponseEntity<String> entity = this.restTemplate.exchange("/", HttpMethod.GET, httpEntity, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE);
	}

}
