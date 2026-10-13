/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.scan.valid.b;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Madhura Bhave
 * @author Stephane Nicoll
 */
public class BScanConfiguration {

	public interface BProperties {

	}

	@ConfigurationProperties("b.first")
	public static class BFirstProperties implements BProperties {

		private final String name;

		public BFirstProperties(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

	}

	@ConfigurationProperties("b.second")
	public static class BSecondProperties implements BProperties {

		private int number;

		public int getNumber() {
			return this.number;
		}

		public void setNumber(int number) {
			this.number = number;
		}

	}

}
