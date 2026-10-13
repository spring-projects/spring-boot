/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.source;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

@TestConfigurationProperties("example")
public class ParentWithHintProperties extends SimpleSource {

	/**
	 * Whether this is enabled.
	 */
	private boolean enabled;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
