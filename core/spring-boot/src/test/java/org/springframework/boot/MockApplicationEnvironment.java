/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot;

import org.jspecify.annotations.Nullable;

import org.springframework.mock.env.MockEnvironment;

/**
 * {@link MockEnvironment} with the same property overrides as
 * {@link ApplicationEnvironment}.
 *
 * @author Phillip Webb
 */
public class MockApplicationEnvironment extends MockEnvironment {

	@Override
	protected @Nullable String doGetActiveProfilesProperty() {
		return null;
	}

	@Override
	protected @Nullable String doGetDefaultProfilesProperty() {
		return null;
	}

}
