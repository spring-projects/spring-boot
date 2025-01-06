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

package org.springframework.boot.actuate.autoconfigure.metrics.export.humio;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.AbstractPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HumioPropertiesConfigAdapter}.
 *
 * @author Andy Wilkinson
 */
class HumioPropertiesConfigAdapterTests
		extends AbstractPropertiesConfigAdapterTests<HumioProperties, HumioPropertiesConfigAdapter> {

	HumioPropertiesConfigAdapterTests() {
		super(HumioPropertiesConfigAdapter.class);
	}

	@Test
	void whenApiTokenIsSetAdapterApiTokenReturnsIt() {
		HumioProperties properties = new HumioProperties();
		properties.setApiToken("ABC123");
		assertThat(new HumioPropertiesConfigAdapter(properties).apiToken()).isEqualTo("ABC123");
	}

	@Test
	void whenPropertiesTagsIsSetAdapterTagsReturnsIt() {
		HumioProperties properties = new HumioProperties();
		properties.setTags(Collections.singletonMap("name", "test"));
		assertThat(new HumioPropertiesConfigAdapter(properties).tags())
			.isEqualTo(Collections.singletonMap("name", "test"));
	}

	@Test
	void whenPropertiesUriIsSetAdapterUriReturnsIt() {
		HumioProperties properties = new HumioProperties();
		properties.setUri("https://humio.example.com");
		assertThat(new HumioPropertiesConfigAdapter(properties).uri()).isEqualTo("https://humio.example.com");
	}

}
