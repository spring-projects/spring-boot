/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

/**
 * POJO for use with samples needing a deprecated value.
 *
 * @author Jared Bates
 */
public class DeprecatedSimplePojo {

	private int value;

	@Deprecated
	public int getValue() {
		return this.value;
	}

	public void setValue(int value) {
		this.value = value;
	}

}
