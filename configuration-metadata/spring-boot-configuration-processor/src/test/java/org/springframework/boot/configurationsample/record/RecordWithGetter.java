/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.record;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

@TestConfigurationProperties("record-with-getter")
public record RecordWithGetter(String alpha) {

	public String getBravo() {
		return this.alpha;
	}

}
