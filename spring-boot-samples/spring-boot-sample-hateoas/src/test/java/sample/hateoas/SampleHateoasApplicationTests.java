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

package sample.hateoas;

import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class SampleHateoasApplicationTests {

	@LocalServerPort
	private int port;

	@Test
	public void hasHalLinks() throws Exception {
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/customers/1", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).startsWith(
				"{\"id\":1,\"firstName\":\"Oliver\"" + ",\"lastName\":\"Gierke\"");
		assertThat(entity.getBody()).contains("_links\":{\"self\":{\"href\"");
	}

	@Test
	public void producesJsonWhenXmlIsPreferred() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.ACCEPT, "application/xml;q=0.9,application/json;q=0.8");
		RequestEntity<?> request = new RequestEntity<Void>(headers, HttpMethod.GET,
				URI.create("http://localhost:" + this.port + "/customers/1"));
		ResponseEntity<String> response = new TestRestTemplate().exchange(request,
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType())
				.isEqualTo(MediaType.parseMediaType("application/json;charset=UTF-8"));
	}

}
