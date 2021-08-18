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

package org.springframework.boot.web.context;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebServerApplicationContext}.
 *
 * @author Phillip Webb
 */
class WebServerApplicationContextTests {

	@Test
	void hasServerNamespaceWhenContextIsNotWebServerApplicationContextReturnsFalse() {
		ApplicationContext context = mock(ApplicationContext.class);
		assertThat(WebServerApplicationContext.hasServerNamespace(context, "test")).isFalse();
	}

	@Test
	void hasServerNamespaceWhenContextIsWebServerApplicationContextAndNamespaceDoesNotMatchReturnsFalse() {
		ApplicationContext context = mock(WebServerApplicationContext.class);
		assertThat(WebServerApplicationContext.hasServerNamespace(context, "test")).isFalse();
	}

	@Test
	void hasServerNamespaceWhenContextIsWebServerApplicationContextAndNamespaceMatchesReturnsTrue() {
		WebServerApplicationContext context = mock(WebServerApplicationContext.class);
		given(context.getServerNamespace()).willReturn("test");
		assertThat(WebServerApplicationContext.hasServerNamespace(context, "test")).isTrue();
	}

}
