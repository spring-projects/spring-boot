/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link WebEndpointProperties}.
 *
 * @author Madhura Bhave
 */
public class WebEndpointPropertiesTests {

	@Test
	public void defaultBasePathShouldBeApplication() {
		WebEndpointProperties properties = new WebEndpointProperties();
		assertThat(properties.getBasePath()).isEqualTo("/actuator");
	}

	@Test
	public void basePathShouldBeCleaned() {
		WebEndpointProperties properties = new WebEndpointProperties();
		properties.setBasePath("/");
		assertThat(properties.getBasePath()).isEqualTo("");
		properties.setBasePath("/actuator/");
		assertThat(properties.getBasePath()).isEqualTo("/actuator");
	}

	@Test
	public void basePathMustStartWithSlash() {
		WebEndpointProperties properties = new WebEndpointProperties();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> properties.setBasePath("admin"))
				.withMessageContaining("Base path must start with '/' or be empty");
	}

	@Test
	public void basePathCanBeEmpty() {
		WebEndpointProperties properties = new WebEndpointProperties();
		properties.setBasePath("");
		assertThat(properties.getBasePath()).isEqualTo("");
	}

}
