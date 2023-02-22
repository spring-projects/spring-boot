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

package org.springframework.boot.test.context.assertj;

import org.junit.jupiter.api.Test;

import org.springframework.boot.web.reactive.context.ConfigurableReactiveWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AssertableReactiveWebApplicationContext}.
 *
 * @author Phillip Webb
 * @see ApplicationContextAssertProviderTests
 */
class AssertableReactiveWebApplicationContextTests {

	@Test
	void getShouldReturnProxy() {
		AssertableReactiveWebApplicationContext context = AssertableReactiveWebApplicationContext
			.get(() -> mock(ConfigurableReactiveWebApplicationContext.class));
		assertThat(context).isInstanceOf(ConfigurableReactiveWebApplicationContext.class);
	}

}
