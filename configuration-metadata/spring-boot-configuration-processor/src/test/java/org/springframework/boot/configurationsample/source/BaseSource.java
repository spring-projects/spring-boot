/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.source;

/**
 * Test properties contributed by a parent source.
 */
public abstract class BaseSource {

	private boolean enabled;

	private String username;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	protected abstract String getPassword();

}
