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

	/**
     * Returns the order of this RestDocsTestExecutionListener.
     * The order is determined by subtracting 100 from the lowest precedence.
     * 
     * @return the order of this RestDocsTestExecutionListener
     */
    @Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 100;
	}

	/**
     * This method is called before each test method is executed.
     * It checks if REST Docs is present and if so, it calls the beforeTestMethod method of the DocumentationHandler class.
     *
     * @param testContext the TestContext object containing information about the test being executed
     * @throws Exception if an error occurs during the execution of the beforeTestMethod method
     */
    @Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		if (REST_DOCS_PRESENT) {
			new DocumentationHandler().beforeTestMethod(testContext);
		}
	}

	/**
     * This method is called after each test method is executed.
     * It checks if REST Docs is present and if so, it calls the afterTestMethod method of the DocumentationHandler class.
     * 
     * @param testContext the TestContext object containing information about the test execution
     * @throws Exception if an error occurs during the execution of the afterTestMethod method
     */
    @Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		if (REST_DOCS_PRESENT) {
			new DocumentationHandler().afterTestMethod(testContext);
		}
	}

	/**
     * DocumentationHandler class.
     */
    private static final class DocumentationHandler {

		/**
         * This method is called before each test method to initialize the ManualRestDocumentation object and
         * invoke the 'beforeTest' method on it if it exists.
         * 
         * @param testContext The TestContext object containing information about the test being executed.
         */
        private void beforeTestMethod(TestContext testContext) {
			ManualRestDocumentation restDocumentation = findManualRestDocumentation(testContext);
			if (restDocumentation != null) {
				restDocumentation.beforeTest(testContext.getTestClass(), testContext.getTestMethod().getName());
			}
		}

		/**
         * This method is called after each test method execution to perform necessary actions for generating documentation.
         * It retrieves the ManualRestDocumentation instance from the TestContext and calls the afterTest() method on it.
         * If the ManualRestDocumentation instance is not found, no action is taken.
         *
         * @param testContext The TestContext object containing information about the current test execution.
         */
        private void afterTestMethod(TestContext testContext) {
			ManualRestDocumentation restDocumentation = findManualRestDocumentation(testContext);
			if (restDocumentation != null) {
				restDocumentation.afterTest();
			}
		}

		/**
         * Finds the ManualRestDocumentation bean from the TestContext's application context.
         * 
         * @param testContext the TestContext object containing the application context
         * @return the ManualRestDocumentation bean if found, null otherwise
         */
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
