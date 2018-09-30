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

package org.springframework.boot.test.context.runner;

import org.junit.Test;

import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebApplicationContextRunner}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public class WebApplicationContextRunnerTests extends
		AbstractApplicationContextRunnerTests<WebApplicationContextRunner, ConfigurableWebApplicationContext, AssertableWebApplicationContext> {

	@Test
	public void contextShouldHaveMockServletContext() {
		get().run((context) -> assertThat(context.getServletContext())
				.isInstanceOf(MockServletContext.class));
	}

	@Override
	protected WebApplicationContextRunner get() {
		return new WebApplicationContextRunner();
	}

}
