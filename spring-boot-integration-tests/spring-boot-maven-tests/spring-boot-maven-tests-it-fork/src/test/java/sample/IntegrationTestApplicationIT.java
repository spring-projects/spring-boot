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

package sample;

import org.junit.Before;
import org.junit.Test;

import org.springframework.web.client.RestTemplate;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author Stephane Nicoll
 */
public class IntegrationTestApplicationIT {

	private int actualServerPort;

	@Before
	public void setup() {
		String port = System.getProperty("test.server.port");
		assertNotNull("System property 'test.server.port' must be set", port);
		this.actualServerPort = Integer.parseInt(port);
	}

	@Test
	public void test() {
		String content = new RestTemplate().getForObject(
				"http://localhost:" + actualServerPort + "/", String.class);
		assertThat(content, is("Hello World!"));
	}

}
