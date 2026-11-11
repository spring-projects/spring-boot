/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.assertj.noCheckReturnValue;

import org.assertj.core.api.AbstractAssert;

public class NoCheckReturnValue extends AbstractAssert<NoCheckReturnValue, Object> {

	NoCheckReturnValue() {
		super(null, NoCheckReturnValue.class);
	}

	public Object notReturningSelf() {
		return new Object();
	}

}
