/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.web.client;

import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.web.client.MockRestServiceServer;

/**
 * {@link TestExecutionListener} to reset {@link MockRestServiceServer} beans.
 *
 * @author Phillip Webb
 */
class MockRestServiceServerResetTestExecutionListener extends AbstractTestExecutionListener {

	/**
     * Returns the order in which this listener should be executed.
     * The order is set to the lowest precedence minus 100.
     *
     * @return the order of execution
     */
    @Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 100;
	}

	/**
     * This method is called after each test method is executed.
     * It resets the MockRestServiceServer instances in the application context.
     * 
     * @param testContext The TestContext object containing information about the test being executed.
     * @throws Exception if an error occurs during the reset process.
     */
    @Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		ApplicationContext applicationContext = testContext.getApplicationContext();
		String[] names = applicationContext.getBeanNamesForType(MockRestServiceServer.class, false, false);
		for (String name : names) {
			applicationContext.getBean(name, MockRestServiceServer.class).reset();
		}
	}

}
