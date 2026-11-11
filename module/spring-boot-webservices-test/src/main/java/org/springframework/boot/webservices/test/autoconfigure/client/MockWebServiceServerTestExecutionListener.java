/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.client;

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
