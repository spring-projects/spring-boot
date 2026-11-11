/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.recursive;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

@TestConfigurationProperties("prefix")
public class RecursiveProperties {

	private RecursiveProperties recursive;

	public RecursiveProperties getRecursive() {
		return this.recursive;
	}

	public void setRecursive(RecursiveProperties recursive) {
		this.recursive = recursive;
	}

}
