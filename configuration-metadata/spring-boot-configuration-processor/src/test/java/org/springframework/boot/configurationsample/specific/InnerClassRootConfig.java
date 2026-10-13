/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Sample with a simple inner class config.
 *
 * @author Stephane Nicoll
 */
public class InnerClassRootConfig {

	@TestConfigurationProperties("config")
	public static class Config {

		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
