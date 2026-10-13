/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.record;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestNestedConfigurationProperty;

@TestConfigurationProperties("record-nested")
public record NestedPropertiesOnMethodRecord(String myProperty, NestedRecord nested, InnerPropertiesRecord inner) {

	@TestNestedConfigurationProperty
	public NestedRecord nested() {
		return this.nested;
	}

	public record InnerPropertiesRecord(String myInnerProperty, NestedRecord nested) {

		@TestNestedConfigurationProperty
		public NestedRecord nested() {
			return this.nested;
		}
	}

}
