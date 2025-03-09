/*
 * Copyright 2025 the original author or authors.
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

package smoketest.armeria.jetty;

import com.linecorp.armeria.server.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Dogac Eldenk
 */
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
class SampleArmeriaJettyApplicationTests {

	@Autowired
	private Server server;

	@Autowired
	private TestRestTemplate restTemplate;

	@BeforeEach
	void setup() {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("http://127.0.0.1:" + server.activeLocalPort());
		this.restTemplate.getRestTemplate().setUriTemplateHandler(factory);
	}

	@Test
	void testJetty() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/jetty", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Hello World");
		assertThat(entity.getHeaders().getFirst("armeria-forwarded")).isEqualTo("true");
	}

	@Test
	void testArmeria() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/armeria", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Hello from Armeria!");
	}

}
