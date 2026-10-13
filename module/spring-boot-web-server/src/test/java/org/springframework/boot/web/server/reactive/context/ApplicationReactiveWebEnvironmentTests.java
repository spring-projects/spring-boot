/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.reactive.context;

import org.springframework.boot.AbstractApplicationEnvironmentTests;
import org.springframework.core.env.StandardEnvironment;

/**
 * Tests for {@link ApplicationReactiveWebEnvironment}.
 *
 * @author Phillip Webb
 */
class ApplicationReactiveWebEnvironmentTests extends AbstractApplicationEnvironmentTests {

	@Override
	protected StandardEnvironment createEnvironment() {
		return new ApplicationReactiveWebEnvironment();
	}

}
