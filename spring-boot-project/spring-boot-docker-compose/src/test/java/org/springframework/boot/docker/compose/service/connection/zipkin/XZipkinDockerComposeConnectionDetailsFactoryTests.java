/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.zipkin;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link ZipkinDockerComposeConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@Disabled
class XZipkinDockerComposeConnectionDetailsFactoryTests {

	@Test
	void test() {
		fail("Not yet implemented");
	}

	// @formatter:off

	/*


	@Test
	void getPort() {
		RunningService service = createService(Collections.emptyMap());
		ZipkinService zipkinService = new ZipkinService(service);
		assertThat(zipkinService.getPort()).isEqualTo(19411);
	}

	@Test
	void matches() {
		assertThat(ZipkinService.matches(createService(Collections.emptyMap()))).isTrue();
		assertThat(ZipkinService.matches(createService(ImageReference.parse("postgres:15.2"), Collections.emptyMap())))
			.isFalse();
	}

	private RunningService createService(Map<String, String> env) {
		return createService(ImageReference.parse("openzipkin/zipkin:2.24"), env);
	}

	private RunningService createService(ImageReference image, Map<String, String> env) {
		return RunningServiceBuilder.create("service-1", image).addTcpPort(9411, 19411).env(env).build();
	}


	 */

	// @formatter:on

}
