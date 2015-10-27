/*
 * Copyright 2012-2015 the original author or authors.
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

package sample.hypermedia.ui;

import java.net.URI;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(SampleHypermediaUiApplication.class)
@WebIntegrationTest(value = { "management.context-path=" }, randomPort = true)
public class SampleHypermediaUiApplicationTests {

	@Value("${local.server.port}")
	private int port;

	@Test
	public void home() {
		String response = new TestRestTemplate()
				.getForObject("http://localhost:" + this.port, String.class);
		assertTrue("Wrong body: " + response, response.contains("Hello World"));
	}

	@Test
	public void links() {
		String response = new TestRestTemplate().getForObject(
				"http://localhost:" + this.port + "/actuator", String.class);
		assertTrue("Wrong body: " + response, response.contains("\"_links\":"));
	}

	@Test
	public void linksWithJson() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		ResponseEntity<String> response = new TestRestTemplate().exchange(
				new RequestEntity<Void>(headers, HttpMethod.GET,
						new URI("http://localhost:" + this.port + "/actuator")),
				String.class);
		assertTrue("Wrong body: " + response, response.getBody().contains("\"_links\":"));
	}

	@Test
	public void homeWithHtml() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		ResponseEntity<String> response = new TestRestTemplate()
				.exchange(new RequestEntity<Void>(headers, HttpMethod.GET,
						new URI("http://localhost:" + this.port)), String.class);
		assertTrue("Wrong body: " + response, response.getBody().contains("Hello World"));
	}

}
