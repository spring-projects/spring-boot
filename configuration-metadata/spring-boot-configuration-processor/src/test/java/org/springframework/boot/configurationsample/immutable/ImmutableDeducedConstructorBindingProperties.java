/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.immutable;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestDefaultValue;

/**
 * @author Madhura Bhave
 */
@TestConfigurationProperties("immutable")
public class ImmutableDeducedConstructorBindingProperties {

	/**
	 * The name of these properties.
	 */
	private final String theName;

	/**
	 * A simple flag.
	 */
	private final boolean flag;

	public ImmutableDeducedConstructorBindingProperties(@TestDefaultValue("boot") String theName, boolean flag) {
		this.theName = theName;
		this.flag = flag;
	}

	public String getTheName() {
		return this.theName;
	}

	public boolean isFlag() {
		return this.flag;
	}

}
