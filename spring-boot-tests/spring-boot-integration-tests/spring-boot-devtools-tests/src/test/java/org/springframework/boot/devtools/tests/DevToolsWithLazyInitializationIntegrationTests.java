/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.devtools.tests;

import java.io.IOException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DevTools with lazy initialization enabled.
 *
 * @author Madhura Bhave
 */
class DevToolsWithLazyInitializationIntegrationTests extends AbstractDevToolsIntegrationTests {

	@ParameterizedTest(name = "{0}")
	@MethodSource("parameters")
	void addARequestMappingToAnExistingControllerWhenLazyInit(ApplicationLauncher applicationLauncher)
			throws Exception {
		launchApplication(applicationLauncher, "--spring.main.lazy-initialization=true");
		TestRestTemplate template = new TestRestTemplate();
		String urlBase = "http://localhost:" + awaitServerPort();
		assertThat(template.getForObject(urlBase + "/one", String.class)).isEqualTo("one");
		assertThat(template.getForEntity(urlBase + "/two", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
		controller("com.example.ControllerOne").withRequestMapping("one").withRequestMapping("two").build();
		urlBase = "http://localhost:" + awaitServerPort();
		assertThat(template.getForObject(urlBase + "/one", String.class)).isEqualTo("one");
		assertThat(template.getForObject(urlBase + "/two", String.class)).isEqualTo("two");
	}

	static Object[] parameters() throws IOException {
		Directories directories = new Directories(buildOutput, temp);
		return new Object[] { new Object[] { new LocalApplicationLauncher(directories) },
				new Object[] { new ExplodedRemoteApplicationLauncher(directories) },
				new Object[] { new JarFileRemoteApplicationLauncher(directories) } };

	}

}
