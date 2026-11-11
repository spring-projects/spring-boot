/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.system;

/**
 * Factory to create mock {@link ApplicationPid} instances for testing.
 *
 * @author Phillip Webb
 */
public final class MockApplicationPid {

	private MockApplicationPid() {
	}

	public static ApplicationPid of(long value) {
		return new ApplicationPid(value);
	}

}
