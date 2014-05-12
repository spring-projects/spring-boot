/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link BasicErrorController} using {@link IntegrationTest} that hit a real
 * HTTP server.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BasicErrorControllerMockMvcTests.TestConfiguration.class)
@WebAppConfiguration
@IntegrationTest("server.port=0")
public class BasicErrorControllerIntegrationTest {

	@Value("${local.server.port}")
	private int port;

	@Test
	@SuppressWarnings("rawtypes")
	public void testErrorForMachineClient() throws Exception {
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port, Map.class);
		assertThat(entity.getBody().toString(), endsWith("status=500, "
				+ "error=Internal Server Error, "
				+ "exception=java.lang.IllegalStateException, " + "message=Expected!, "
				+ "path=/}"));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testBindingExceptionForMachineClient() throws Exception {
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/bind", Map.class);
		String resp = entity.getBody().toString();
		assertThat(resp, containsString("Error count: 1"));
		assertThat(resp, containsString("errors=[{codes="));
		assertThat(resp, containsString("org.springframework.validation.BindException"));
	}
}
