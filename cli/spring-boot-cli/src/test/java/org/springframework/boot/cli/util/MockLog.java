/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli.util;

import static org.mockito.Mockito.mock;

/**
 * Mock log for testing message output.
 *
 * @author Phillip Webb
 */
public interface MockLog extends LogListener {

	static MockLog attach() {
		MockLog log = mock(MockLog.class);
		Log.setListener(log);
		return log;
	}

	static void clear() {
		Log.setListener(null);
	}

}
