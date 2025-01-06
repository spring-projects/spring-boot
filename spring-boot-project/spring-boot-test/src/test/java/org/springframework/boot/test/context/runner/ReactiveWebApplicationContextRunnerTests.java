/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.test.context.runner;

import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebApplicationContext;
import org.springframework.boot.web.reactive.context.ConfigurableReactiveWebApplicationContext;

/**
 * Tests for {@link ReactiveWebApplicationContextRunner}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ReactiveWebApplicationContextRunnerTests extends
		AbstractApplicationContextRunnerTests<ReactiveWebApplicationContextRunner, ConfigurableReactiveWebApplicationContext, AssertableReactiveWebApplicationContext> {

	@Override
	protected ReactiveWebApplicationContextRunner get() {
		return new ReactiveWebApplicationContextRunner();
	}

	@Override
	protected ReactiveWebApplicationContextRunner getWithAdditionalContextInterface() {
		return new ReactiveWebApplicationContextRunner(TestAnnotationConfigReactiveWebApplicationContext::new,
				AdditionalContextInterface.class);
	}

	static class TestAnnotationConfigReactiveWebApplicationContext extends AnnotationConfigReactiveWebApplicationContext
			implements AdditionalContextInterface {

	}

}
