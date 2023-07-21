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

package org.springframework.boot.actuate.autoconfigure.web.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ManagementServerProperties}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class ManagementServerPropertiesTests {

	@Test
	void defaultPortIsNull() {
		ManagementServerProperties properties = new ManagementServerProperties();
		assertThat(properties.getPort()).isNull();
	}

	@Test
	void definedPort() {
		ManagementServerProperties properties = new ManagementServerProperties();
		properties.setPort(123);
		assertThat(properties.getPort()).isEqualTo(123);
	}

	@Test
	void defaultBasePathIsEmptyString() {
		ManagementServerProperties properties = new ManagementServerProperties();
		assertThat(properties.getBasePath()).isEmpty();
	}

	@Test
	void definedBasePath() {
		ManagementServerProperties properties = new ManagementServerProperties();
		properties.setBasePath("/foo");
		assertThat(properties.getBasePath()).isEqualTo("/foo");
	}

	@Test
	void trailingSlashOfBasePathIsRemoved() {
		ManagementServerProperties properties = new ManagementServerProperties();
		properties.setBasePath("/foo/");
		assertThat(properties.getBasePath()).isEqualTo("/foo");
	}

	@Test
	void slashOfBasePathIsDefaultValue() {
		ManagementServerProperties properties = new ManagementServerProperties();
		properties.setBasePath("/");
		assertThat(properties.getBasePath()).isEmpty();
	}

}
