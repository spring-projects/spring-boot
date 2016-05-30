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

package sample.hypermedia.ui;

import java.net.URI;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
		"management.context-path=" })
public class SampleHypermediaUiApplicationTests {

	@LocalServerPort
	private int port;

	@Test
	public void home() {
		String response = new TestRestTemplate()
				.getForObject("http://localhost:" + this.port, String.class);
		assertThat(response).contains("Hello World");
	}

	@Test
	public void links() {
		String response = new TestRestTemplate().getForObject(
				"http://localhost:" + this.port + "/actuator", String.class);
		assertThat(response).contains("\"_links\":");
	}

	@Test
	public void linksWithJson() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		ResponseEntity<String> response = new TestRestTemplate().exchange(
				new RequestEntity<Void>(headers, HttpMethod.GET,
						new URI("http://localhost:" + this.port + "/actuator")),
				String.class);
		assertThat(response.getBody()).contains("\"_links\":");
	}

	@Test
	public void homeWithHtml() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		ResponseEntity<String> response = new TestRestTemplate()
				.exchange(new RequestEntity<Void>(headers, HttpMethod.GET,
						new URI("http://localhost:" + this.port)), String.class);
		assertThat(response.getBody()).contains("Hello World");
	}

}
