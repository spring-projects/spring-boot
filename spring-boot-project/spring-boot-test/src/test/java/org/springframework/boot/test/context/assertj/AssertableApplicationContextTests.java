/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.context.assertj;

import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AssertableApplicationContext}.
 *
 * @author Phillip Webb
 * @see ApplicationContextAssertProviderTests
 */
public class AssertableApplicationContextTests {

	@Test
	public void getShouldReturnProxy() {
		AssertableApplicationContext context = AssertableApplicationContext
				.get(() -> mock(ConfigurableApplicationContext.class));
		assertThat(context).isInstanceOf(ConfigurableApplicationContext.class);
	}

}
