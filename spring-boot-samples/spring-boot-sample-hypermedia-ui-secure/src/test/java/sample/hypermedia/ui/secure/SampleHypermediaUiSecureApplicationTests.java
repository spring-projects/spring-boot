/*
 * Copyright 2012-2018 the original author or authors.
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

package sample.hypermedia.ui.secure;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"endpoints.env.sensitive=false", "foo=bar" })
public class SampleHypermediaUiSecureApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void links() {
		String response = this.restTemplate.getForObject("/actuator", String.class);
		assertThat(response).contains("\"_links\":");
	}

	@Test
	public void testInsecureNestedPath() throws Exception {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/env",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		ResponseEntity<String> user = this.restTemplate.getForEntity("/env/foo",
				String.class);
		assertThat(user.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(user.getBody()).contains("{\"foo\":");
	}

	@Test
	public void testSecurePath() throws Exception {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/metrics",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

}
