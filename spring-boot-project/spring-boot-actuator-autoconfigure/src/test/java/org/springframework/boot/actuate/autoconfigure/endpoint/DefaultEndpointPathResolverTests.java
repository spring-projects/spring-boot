/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.web.EndpointPathResolver;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultEndpointPathResolver}.
 *
 * @author Stephane Nicoll
 */
public class DefaultEndpointPathResolverTests {

	private final MockEnvironment environment = new MockEnvironment();

	private final EndpointPathResolver resolver = new DefaultEndpointPathResolver(
			this.environment);

	@Test
	public void defaultConfiguration() {
		assertThat(this.resolver.resolvePath("test")).isEqualTo("test");
	}

	@Test
	public void userConfiguration() {
		this.environment.setProperty("endpoints.test.web.path", "custom");
		assertThat(this.resolver.resolvePath("test")).isEqualTo("custom");
	}

}
