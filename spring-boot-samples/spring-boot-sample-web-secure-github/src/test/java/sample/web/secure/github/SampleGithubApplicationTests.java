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

package sample.web.secure.github;

import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for GitHub SSO application.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class SampleGithubApplicationTests {

	@LocalServerPort
	private int port;

	@Test
	public void everythingIsSecuredByDefault() throws Exception {
		TestRestTemplate restTemplate = new TestRestTemplate();
		ResponseEntity<Void> entity = restTemplate
				.getForEntity("http://localhost:" + this.port, Void.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FOUND);
		assertThat(entity.getHeaders().getLocation())
				.isEqualTo(URI.create("http://localhost:" + this.port + "/login"));
	}

	@Test
	public void loginRedirectsToGithub() throws Exception {
		TestRestTemplate restTemplate = new TestRestTemplate();
		ResponseEntity<Void> entity = restTemplate
				.getForEntity("http://localhost:" + this.port + "/login", Void.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FOUND);
		assertThat(entity.getHeaders().getLocation().toString())
				.startsWith("https://github.com/login/oauth");
	}

}
