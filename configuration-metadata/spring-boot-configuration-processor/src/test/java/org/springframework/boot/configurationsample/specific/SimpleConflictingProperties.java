/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.simple.SimpleProperties;

/**
 * Properties that conflict with {@link SimpleProperties}.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("simple")
public class SimpleConflictingProperties {

	private String flag = "hello";

	public String getFlag() {
		return this.flag;
	}

	public void setFlag(String flag) {
		this.flag = flag;
	}

}
