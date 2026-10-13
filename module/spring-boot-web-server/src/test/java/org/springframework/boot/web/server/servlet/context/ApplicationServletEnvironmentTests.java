/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.servlet.context;

import org.springframework.boot.AbstractApplicationEnvironmentTests;
import org.springframework.boot.web.context.servlet.ApplicationServletEnvironment;
import org.springframework.core.env.StandardEnvironment;

/**
 * Tests for {@link ApplicationServletEnvironment}.
 *
 * @author Phillip Webb
 */
class ApplicationServletEnvironmentTests extends AbstractApplicationEnvironmentTests {

	@Override
	protected StandardEnvironment createEnvironment() {
		return new ApplicationServletEnvironment();
	}

}
