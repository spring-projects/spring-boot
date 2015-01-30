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

package sample.undertow;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.assertEquals;

/**
 * Basic integration tests for demo application.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleUndertowApplication.class)
@WebAppConfiguration
@IntegrationTest("server.port:0")
@DirtiesContext
public class SampleUndertowApplicationTests {

	@Value("${local.server.port}")
	private int port;

	@Test
	public void testHome() throws Exception {
		assertOkResponse("/", "Hello World");
	}

	@Test
	public void testAsync() throws Exception {
		assertOkResponse("/async", "async: Hello World");
	}

	private void assertOkResponse(String path, String body) {
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + path, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals(body, entity.getBody());
	}

}
