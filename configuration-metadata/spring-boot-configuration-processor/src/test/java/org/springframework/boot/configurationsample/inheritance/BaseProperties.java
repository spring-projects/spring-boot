/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.inheritance;

public class BaseProperties {

	private boolean boolValue;

	private int intValue;

	private final Nest nest = new Nest();

	public boolean isBoolValue() {
		return this.boolValue;
	}

	public void setBoolValue(boolean boolValue) {
		this.boolValue = boolValue;
	}

	public int getIntValue() {
		return this.intValue;
	}

	public void setIntValue(int intValue) {
		this.intValue = intValue;
	}

	public Nest getNest() {
		return this.nest;
	}

	public static class Nest {

		private boolean boolValue;

		private int intValue;

		public boolean isBoolValue() {
			return this.boolValue;
		}

		public void setBoolValue(boolean boolValue) {
			this.boolValue = boolValue;
		}

		public int getIntValue() {
			return this.intValue;
		}

		public void setIntValue(int intValue) {
			this.intValue = intValue;
		}

	}

}
