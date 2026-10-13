/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.test.autoconfigure;

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

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 100;
	}

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		ApplicationContext applicationContext = testContext.getApplicationContext();
		String[] names = applicationContext.getBeanNamesForType(MockRestServiceServer.class, false, false);
		for (String name : names) {
			applicationContext.getBean(name, MockRestServiceServer.class).reset();
		}
	}

}
