/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.record;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestNestedConfigurationProperty;

@TestConfigurationProperties("record-nested")
public record NestedPropertiesRecord(String myProperty, @TestNestedConfigurationProperty NestedRecord nested,
		InnerPropertiesRecord inner) {

	public record InnerPropertiesRecord(String myInnerProperty, @TestNestedConfigurationProperty NestedRecord nested) {
	}

}
