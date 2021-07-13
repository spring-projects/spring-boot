/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebServerNamespace}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class WebServerNamespaceTests {

	@Test
	void fromWhenValueHasText() {
		assertThat(WebServerNamespace.from("management")).isEqualTo(WebServerNamespace.MANAGEMENT);
	}

	@Test
	void fromWhenValueIsNull() {
		assertThat(WebServerNamespace.from(null)).isEqualTo(WebServerNamespace.SERVER);
	}

	@Test
	void fromWhenValueIsEmpty() {
		assertThat(WebServerNamespace.from("")).isEqualTo(WebServerNamespace.SERVER);
	}

	@Test
	void namespaceWithSameValueAreEqual() {
		assertThat(WebServerNamespace.from("value")).isEqualTo(WebServerNamespace.from("value"));
	}

	@Test
	void namespaceWithDifferentValuesAreNotEqual() {
		assertThat(WebServerNamespace.from("value")).isNotEqualTo(WebServerNamespace.from("other"));
	}

}
