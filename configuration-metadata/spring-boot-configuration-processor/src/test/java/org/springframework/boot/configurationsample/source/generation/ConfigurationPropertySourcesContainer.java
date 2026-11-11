/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.source.generation;

import org.springframework.boot.configurationsample.TestConfigurationPropertiesSource;

public class ConfigurationPropertySourcesContainer {

	@TestConfigurationPropertiesSource
	public static class First {

		/**
		 * A name.
		 */
		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@TestConfigurationPropertiesSource
	public static class Second {

		/**
		 * Whether this is visible.
		 */
		private boolean visible = true;

		public boolean isVisible() {
			return this.visible;
		}

		public void setVisible(boolean visible) {
			this.visible = visible;
		}

	}

	public static class Third {

		private String shouldBeIgnored;

		public String getShouldBeIgnored() {
			return this.shouldBeIgnored;
		}

		public void setShouldBeIgnored(String shouldBeIgnored) {
			this.shouldBeIgnored = shouldBeIgnored;
		}

	}

}
