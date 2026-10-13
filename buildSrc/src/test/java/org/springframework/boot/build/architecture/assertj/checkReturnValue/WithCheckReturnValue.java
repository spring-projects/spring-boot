/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.assertj.checkReturnValue;

import org.assertj.core.api.AbstractAssert;

import org.springframework.lang.CheckReturnValue;

public class WithCheckReturnValue extends AbstractAssert<WithCheckReturnValue, Object> {

	WithCheckReturnValue() {
		super(null, WithCheckReturnValue.class);
	}

	@CheckReturnValue
	public Object notReturningSelf() {
		return new Object();
	}

	@Override
	public WithCheckReturnValue isEqualTo(Object expected) {
		return super.isEqualTo(expected);
	}

}
