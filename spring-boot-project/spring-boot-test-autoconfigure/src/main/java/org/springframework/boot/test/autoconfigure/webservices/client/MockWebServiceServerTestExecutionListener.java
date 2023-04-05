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

package org.springframework.boot.test.autoconfigure.webservices.client;

import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ClassUtils;
import org.springframework.ws.test.client.MockWebServiceServer;

/**
 * {@link TestExecutionListener} to {@code verify} and {@code reset}
 * {@link MockWebServiceServer}.
 *
 * @author Dmytro Nosan
 * @since 2.3.0
 */
public class MockWebServiceServerTestExecutionListener extends AbstractTestExecutionListener {

	private static final boolean MOCK_SERVER_PRESENT = ClassUtils.isPresent(
			"org.springframework.ws.test.client.MockWebServiceServer",
			MockWebServiceServerTestExecutionListener.class.getClassLoader());

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 100;
	}

	@Override
	public void afterTestMethod(TestContext testContext) {
		if (MOCK_SERVER_PRESENT) {
			ApplicationContext applicationContext = testContext.getApplicationContext();
			String[] names = applicationContext.getBeanNamesForType(MockWebServiceServer.class, false, false);
			for (String name : names) {
				MockWebServiceServer mockServer = applicationContext.getBean(name, MockWebServiceServer.class);
				mockServer.verify();
				mockServer.reset();
			}
		}
	}

}
