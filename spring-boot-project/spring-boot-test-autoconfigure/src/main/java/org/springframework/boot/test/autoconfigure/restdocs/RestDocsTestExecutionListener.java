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

package org.springframework.boot.test.autoconfigure.restdocs;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.Ordered;
import org.springframework.restdocs.ManualRestDocumentation;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ClassUtils;

/**
 * A {@link TestExecutionListener} for Spring REST Docs that removes the need for a
 * {@code @Rule} when using JUnit or manual before and after test calls when using TestNG.
 *
 * @author Andy Wilkinson
 * @since 1.4.0
 */
public class RestDocsTestExecutionListener extends AbstractTestExecutionListener {

	private static final boolean REST_DOCS_PRESENT = ClassUtils.isPresent(
			"org.springframework.restdocs.ManualRestDocumentation",
			RestDocsTestExecutionListener.class.getClassLoader());

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 100;
	}

	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		if (REST_DOCS_PRESENT) {
			new DocumentationHandler().beforeTestMethod(testContext);
		}
	}

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		if (REST_DOCS_PRESENT) {
			new DocumentationHandler().afterTestMethod(testContext);
		}
	}

	private static class DocumentationHandler {

		private void beforeTestMethod(TestContext testContext) {
			ManualRestDocumentation restDocumentation = findManualRestDocumentation(testContext);
			if (restDocumentation != null) {
				restDocumentation.beforeTest(testContext.getTestClass(), testContext.getTestMethod().getName());
			}
		}

		private void afterTestMethod(TestContext testContext) {
			ManualRestDocumentation restDocumentation = findManualRestDocumentation(testContext);
			if (restDocumentation != null) {
				restDocumentation.afterTest();
			}
		}

		private ManualRestDocumentation findManualRestDocumentation(TestContext testContext) {
			try {
				return testContext.getApplicationContext().getBean(ManualRestDocumentation.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				return null;
			}
		}

	}

}
