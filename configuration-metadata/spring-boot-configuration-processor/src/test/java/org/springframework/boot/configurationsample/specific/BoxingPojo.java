/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Demonstrate the use of boxing/unboxing. Even if the type does not strictly match, it
 * should still be detected.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("boxing")
public class BoxingPojo {

	private boolean flag;

	private Boolean anotherFlag;

	private Integer counter;

	public boolean isFlag() {
		return this.flag;
	}

	// Setter use Boolean
	public void setFlag(Boolean flag) {
		this.flag = flag;
	}

	public boolean isAnotherFlag() {
		return Boolean.TRUE.equals(this.anotherFlag);
	}

	public void setAnotherFlag(boolean anotherFlag) {
		this.anotherFlag = anotherFlag;
	}

	public Integer getCounter() {
		return this.counter;
	}

	// Setter use int
	public void setCounter(int counter) {
		this.counter = counter;
	}

}
