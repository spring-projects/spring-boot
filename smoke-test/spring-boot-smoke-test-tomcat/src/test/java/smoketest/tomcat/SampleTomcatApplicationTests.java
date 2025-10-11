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

package smoketest.tomcat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import smoketest.tomcat.util.RandomStringUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Florian Storz
 * @author Michael Weidmann
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
@AutoConfigureTestRestTemplate
class SampleTomcatApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private ApplicationContext applicationContext;

	@Value("${server.max-http-request-header-size}")
	private int maxHttpRequestHeaderSize;

	@Test
	void testHome() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().get(HttpHeaders.CONTENT_ENCODING)).isNull();
		assertThat(entity.getBody()).isEqualTo("Hello World");
	}

	@Test
	void testCompression() throws IOException {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept-Encoding", "gzip");
		HttpEntity<?> requestEntity = new HttpEntity<>(requestHeaders);
		ResponseEntity<byte[]> entity = this.restTemplate.exchange("/", HttpMethod.GET, requestEntity, byte[].class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().get(HttpHeaders.CONTENT_ENCODING)).containsExactly("gzip");
		try (GZIPInputStream inflater = new GZIPInputStream(new ByteArrayInputStream(entity.getBody()))) {
			assertThat(StreamUtils.copyToString(inflater, StandardCharsets.UTF_8)).isEqualTo("Hello World");
		}
	}

	@Test
	void testTimeout() {
		ServletWebServerApplicationContext context = (ServletWebServerApplicationContext) this.applicationContext;
		TomcatWebServer embeddedServletContainer = (TomcatWebServer) context.getWebServer();
		ProtocolHandler protocolHandler = embeddedServletContainer.getTomcat().getConnector().getProtocolHandler();
		int timeout = ((AbstractProtocol<?>) protocolHandler).getConnectionTimeout();
		assertThat(timeout).isEqualTo(5000);
	}

	@Test
	void testMaxHttpResponseHeaderSize(CapturedOutput output) {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/max-http-response-header", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(output).contains(
				"threw exception [Request processing failed: org.apache.coyote.http11.HeadersTooLargeException: An attempt was made to write more data to the response headers than there was room available in the buffer. Increase maxHttpHeaderSize on the connector or write less data into the response headers.]");
	}

	@Test
	void testMaxHttpRequestHeaderSize(CapturedOutput output) {
		String headerValue = RandomStringUtil.getRandomBase64EncodedString(this.maxHttpRequestHeaderSize + 1);
		HttpHeaders headers = new HttpHeaders();
		headers.add("x-max-request-header", headerValue);
		HttpEntity<?> httpEntity = new HttpEntity<>(headers);
		ResponseEntity<String> entity = this.restTemplate.exchange("/", HttpMethod.GET, httpEntity, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(output).contains("java.lang.IllegalArgumentException: Request header is too large");
	}

	@TestConfiguration
	static class DisableCompressionConfiguration {

		@Bean
		RestTemplateBuilder restTemplateBuilder() {
			return new RestTemplateBuilder().requestFactoryBuilder(ClientHttpRequestFactoryBuilder.jdk()
				.withCustomizer((factory) -> factory.enableCompression(false)));
		}

	}

}
