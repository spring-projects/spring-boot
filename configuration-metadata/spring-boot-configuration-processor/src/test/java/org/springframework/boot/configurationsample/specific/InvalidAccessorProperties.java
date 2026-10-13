/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Demonstrates that invalid accessors are ignored.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("config")
public class InvalidAccessorProperties {

	private String name;

	private boolean flag;

	public void set(String name) {
		this.name = name;
	}

	public String get() {
		return this.name;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	public boolean is() {
		return this.flag;
	}

}
