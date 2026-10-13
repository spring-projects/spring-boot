/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.log4j2;

import java.io.IOException;

import org.springframework.boot.logging.StackTracePrinter;
import org.springframework.util.ClassUtils;

/**
 * Simple {@link StackTracePrinter} used for testing.
 *
 * @author Phillip Webb
 */
class SimpleStackTracePrinter implements StackTracePrinter {

	@Override
	public void printStackTrace(Throwable throwable, Appendable out) throws IOException {
		out.append("stacktrace:" + ClassUtils.getShortName(throwable.getClass()));
	}

}
