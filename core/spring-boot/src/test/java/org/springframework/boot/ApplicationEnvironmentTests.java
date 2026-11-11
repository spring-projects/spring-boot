/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot;

import org.springframework.core.env.StandardEnvironment;

/**
 * Tests for {@link ApplicationEnvironment}.
 *
 * @author Phillip Webb
 */
class ApplicationEnvironmentTests extends AbstractApplicationEnvironmentTests {

	@Override
	protected StandardEnvironment createEnvironment() {
		return new ApplicationEnvironment();
	}

}
