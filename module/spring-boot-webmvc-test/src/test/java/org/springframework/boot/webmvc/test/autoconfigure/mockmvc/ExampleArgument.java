/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

/**
 * @author Phillip Webb
 */
public class ExampleArgument {

	private final String value;

	public ExampleArgument(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return this.value;
	}

}
