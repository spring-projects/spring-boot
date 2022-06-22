package org.springframework.boot.configurationsample.inheritance;

public class OverrideChildProperties extends BaseProperties {

	private long longValue;

	private final CustomNest nest = new CustomNest();

	public long getLongValue() {
		return this.longValue;
	}

	public void setLongValue(long longValue) {
		this.longValue = longValue;
	}

	@Override
	public CustomNest getNest() {
		return this.nest;
	}

	public static class CustomNest extends Nest {

		private long longValue;

		public long getLongValue() {
			return this.longValue;
		}

		public void setLongValue(long longValue) {
			this.longValue = longValue;
		}

	}

}