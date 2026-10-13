/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Demonstrate that an unrelated setter is not taken into account to detect the deprecated
 * flag.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("not.deprecated")
public class DeprecatedUnrelatedMethodPojo {

	private Integer counter;

	private boolean flag;

	public Integer getCounter() {
		return this.counter;
	}

	public void setCounter(Integer counter) {
		this.counter = counter;
	}

	@Deprecated
	public void setCounter(String counterAsString) {
		this.counter = Integer.valueOf(counterAsString);
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
