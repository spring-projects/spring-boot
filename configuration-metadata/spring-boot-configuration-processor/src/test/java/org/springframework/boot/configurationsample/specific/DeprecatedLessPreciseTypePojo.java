/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Demonstrate that deprecating accessor with not the same type is not taken into account
 * to detect the deprecated flag.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("not.deprecated")
public class DeprecatedLessPreciseTypePojo {

	private boolean flag;

	@Deprecated
	public Boolean getFlag() {
		return this.flag;
	}

	public boolean isFlag() {
		return this.flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	@Deprecated
	public void setFlag(Boolean flag) {
		this.flag = flag;
	}

}
