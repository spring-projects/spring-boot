/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure;

import org.springframework.boot.webmvc.test.autoconfigure.SpringBootMockMvcBuilderCustomizer.DeferredLinesWriter;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * {@link TestExecutionListener} used to print MVC lines only on failure.
 *
 * @author Phillip Webb
 */
class MockMvcPrintOnlyOnFailureTestExecutionListener extends AbstractTestExecutionListener {

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 100;
	}

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		DeferredLinesWriter writer = DeferredLinesWriter.get(testContext.getApplicationContext());
		if (writer != null) {
			if (testContext.getTestException() != null) {
				writer.writeDeferredResult();
			}
			writer.clear();
		}

	}

}
