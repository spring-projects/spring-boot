/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.inheritance;

public class ChildProperties extends BaseProperties {

	private long longValue;

	private final NestInChild childNest = new NestInChild();

	public long getLongValue() {
		return this.longValue;
	}

	public void setLongValue(long longValue) {
		this.longValue = longValue;
	}

	public NestInChild getChildNest() {
		return this.childNest;
	}

	public static class NestInChild {

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
