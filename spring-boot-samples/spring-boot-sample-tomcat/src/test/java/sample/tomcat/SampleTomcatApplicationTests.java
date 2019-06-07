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

package sample.tomcat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class SampleTomcatApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void testHome() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Hello World");
	}

	@Test
	public void testCompression() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept-Encoding", "gzip");
		HttpEntity<?> requestEntity = new HttpEntity<>(requestHeaders);
		ResponseEntity<byte[]> entity = this.restTemplate.exchange("/", HttpMethod.GET, requestEntity, byte[].class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		try (GZIPInputStream inflater = new GZIPInputStream(new ByteArrayInputStream(entity.getBody()))) {
			assertThat(StreamUtils.copyToString(inflater, StandardCharsets.UTF_8)).isEqualTo("Hello World");
		}
	}

	@Test
	public void testTimeout() {
		ServletWebServerApplicationContext context = (ServletWebServerApplicationContext) this.applicationContext;
		TomcatWebServer embeddedServletContainer = (TomcatWebServer) context.getWebServer();
		ProtocolHandler protocolHandler = embeddedServletContainer.getTomcat().getConnector().getProtocolHandler();
		int timeout = ((AbstractProtocol<?>) protocolHandler).getConnectionTimeout();
		assertThat(timeout).isEqualTo(5000);
	}

}
