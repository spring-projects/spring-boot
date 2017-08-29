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

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.web.ManagementServerProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ManagementEndpointPathResolver}.
 *
 * @author Madhura Bhave
 */
public class ManagementEndpointPathResolverTests {

	private ManagementEndpointPathResolver resolver;

	@Before
	public void setUp() throws Exception {
		ManagementServerProperties properties = new ManagementServerProperties();
		properties.setContextPath("/test");
		this.resolver = new ManagementEndpointPathResolver(properties);

	}

	@Test
	public void resolveShouldReturnPathBasedOnContextPath() throws Exception {
		String path = this.resolver.resolvePath("my-id");
		assertThat(path.equals("/test/my-id"));
	}

}
