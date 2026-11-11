/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.autoconfigure;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Base class for tests for {@link DevToolsDataSourceAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
abstract class AbstractDevToolsAutoConfigurationTests {

	/**
	 * Create an {@link AssertableApplicationContext} from the given {@code supplier}.
	 * <p>
	 * Devtools auto-configuration backs-off when it detects JUnit in the call stack. This
	 * method initializes the context in a separate thread to ignore that particular
	 * condition.
	 * @param supplier a supplier of the context
	 * @return an assertable context
	 */
	protected AssertableApplicationContext getContext(Supplier<ConfigurableApplicationContext> supplier)
			throws Exception {
		AtomicReference<ConfigurableApplicationContext> atomicReference = new AtomicReference<>();
		Thread thread = new Thread(() -> {
			ConfigurableApplicationContext context = supplier.get();
			atomicReference.getAndSet(context);
		});
		thread.start();
		thread.join();
		return AssertableApplicationContext.get(atomicReference::get);
	}

}
