/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.method;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestNestedConfigurationProperty;

@TestConfigurationProperties("method-nested")
public class NestedPropertiesMethod {

	private String myProperty;

	private final NestedProperty nested = new NestedProperty();

	private final Inner inner = new Inner();

	public String getMyProperty() {
		return this.myProperty;
	}

	public void setMyProperty(String myProperty) {
		this.myProperty = myProperty;
	}

	@TestNestedConfigurationProperty
	public NestedProperty getNested() {
		return this.nested;
	}

	public Inner getInner() {
		return this.inner;
	}

	public static class Inner {

		private String myInnerProperty;

		private final NestedProperty nested = new NestedProperty();

		public String getMyInnerProperty() {
			return this.myInnerProperty;
		}

		public void setMyInnerProperty(String myInnerProperty) {
			this.myInnerProperty = myInnerProperty;
		}

		@TestNestedConfigurationProperty
		public NestedProperty getNested() {
			return this.nested;
		}

	}

}
