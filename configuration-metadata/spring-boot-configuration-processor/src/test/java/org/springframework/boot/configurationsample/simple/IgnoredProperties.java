/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.simple;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Configuration properties where some of them are being ignored.
 *
 * @author Moritz Halbritter
 */
@TestConfigurationProperties("ignored")
public class IgnoredProperties {

	private String prop1;

	private String prop2;

	private String prop3;

	public String getProp1() {
		return this.prop1;
	}

	public void setProp1(String prop1) {
		this.prop1 = prop1;
	}

	public String getProp2() {
		return this.prop2;
	}

	public void setProp2(String prop2) {
		this.prop2 = prop2;
	}

	public String getProp3() {
		return this.prop3;
	}

	public void setProp3(String prop3) {
		this.prop3 = prop3;
	}

}
