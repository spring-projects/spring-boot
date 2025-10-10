/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.webmvc.test.autoconfigure;

import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * {@link TestExecutionListener} to reset the {@link WebDriverScope}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see WebDriverContextCustomizerFactory
 * @see WebDriverScope
 */
public class WebDriverTestExecutionListener extends AbstractTestExecutionListener {

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 100;
	}

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		WebDriverScope scope = WebDriverScope.getFrom(testContext.getApplicationContext());
		if (scope != null && scope.reset()) {
			testContext.setAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE,
					Boolean.TRUE);
		}
	}

}
