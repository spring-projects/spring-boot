/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.source;

public class ConcreteSource extends BaseSource {

	/**
	 * Password.
	 */
	private String password;

	@Override
	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
