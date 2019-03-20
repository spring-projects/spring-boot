/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.context.embedded;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Spring Boot's embedded servlet container support using jar
 * packaging.
 *
 * @author Andy Wilkinson
 */
@RunWith(Parameterized.class)
public class EmbeddedServletContainerJarPackagingIntegrationTests
		extends AbstractEmbeddedServletContainerIntegrationTests {

	@Parameters(name = "{0}")
	public static Object[] parameters() {
		return AbstractEmbeddedServletContainerIntegrationTests.parameters("jar",
				Arrays.asList(PackagedApplicationLauncher.class,
						ExplodedApplicationLauncher.class));
	}

	public EmbeddedServletContainerJarPackagingIntegrationTests(String name,
			AbstractApplicationLauncher launcher) {
		super(name, launcher);
	}

	@Test
	public void nestedMetaInfResourceIsAvailableViaHttp() throws Exception {
		ResponseEntity<String> entity = this.rest
				.getForEntity("/nested-meta-inf-resource.txt", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void nestedMetaInfResourceIsAvailableViaServletContext() throws Exception {
		ResponseEntity<String> entity = this.rest.getForEntity(
				"/servletContext?/nested-meta-inf-resource.txt", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void nestedJarIsNotAvailableViaHttp() throws Exception {
		ResponseEntity<String> entity = this.rest
				.getForEntity("/BOOT-INF/lib/resources-1.0.jar", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void applicationClassesAreNotAvailableViaHttp() throws Exception {
		ResponseEntity<String> entity = this.rest.getForEntity(
				"/BOOT-INF/classes/com/example/ResourceHandlingApplication.class",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void launcherIsNotAvailableViaHttp() throws Exception {
		ResponseEntity<String> entity = this.rest.getForEntity(
				"/org/springframework/boot/loader/Launcher.class", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

}
