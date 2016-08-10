/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.jetty92;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
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
@DirtiesContext
public class SampleJetty92ApplicationTests {

	@LocalServerPort
	private int port;

	@Test
	public void testHome() throws Exception {
		ResponseEntity<String> entity = new TestRestTemplate()
				.getForEntity("http://localhost:" + this.port, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Hello World");
	}

	@Test
	public void testCompression() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Accept-Encoding", "gzip");
		HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
		TestRestTemplate restTemplate = new TestRestTemplate();
		ResponseEntity<byte[]> entity = restTemplate.exchange(
				"http://localhost:" + this.port, HttpMethod.GET, requestEntity,
				byte[].class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		GZIPInputStream inflater = new GZIPInputStream(
				new ByteArrayInputStream(entity.getBody()));
		try {
			assertThat(StreamUtils.copyToString(inflater, Charset.forName("UTF-8")))
					.isEqualTo("Hello World");
		}
		finally {
			inflater.close();
		}
	}

}
