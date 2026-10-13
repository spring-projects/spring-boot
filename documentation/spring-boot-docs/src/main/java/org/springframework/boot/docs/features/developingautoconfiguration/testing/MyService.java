/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.developingautoconfiguration.testing;

public class MyService {

	private final String name;

	public MyService(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

}
