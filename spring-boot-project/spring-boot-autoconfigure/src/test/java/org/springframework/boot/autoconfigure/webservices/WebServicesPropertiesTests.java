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

package org.springframework.boot.autoconfigure.webservices;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link WebServicesProperties}.
 *
 * @author Madhura Bhave
 */
class WebServicesPropertiesTests {

	private WebServicesProperties properties;

	@Test
	void pathMustNotBeEmpty() {
		this.properties = new WebServicesProperties();
		assertThatIllegalArgumentException().isThrownBy(() -> this.properties.setPath(""))
			.withMessageContaining("Path must have length greater than 1");
	}

	@Test
	void pathMustHaveLengthGreaterThanOne() {
		this.properties = new WebServicesProperties();
		assertThatIllegalArgumentException().isThrownBy(() -> this.properties.setPath("/"))
			.withMessageContaining("Path must have length greater than 1");
	}

	@Test
	void customPathMustBeginWithASlash() {
		this.properties = new WebServicesProperties();
		assertThatIllegalArgumentException().isThrownBy(() -> this.properties.setPath("custom"))
			.withMessageContaining("Path must start with '/'");
	}

}
