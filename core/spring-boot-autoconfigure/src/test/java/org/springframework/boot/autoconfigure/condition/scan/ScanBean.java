/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition.scan;

public class ScanBean {

	private final String value;

	public ScanBean(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return this.value;
	}

}
