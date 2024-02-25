/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.h2;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * Configuration properties for H2's console.
 *
 * @author Andy Wilkinson
 * @author Marten Deinum
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.h2.console")
public class H2ConsoleProperties {

	/**
	 * Path at which the console is available.
	 */
	private String path = "/h2-console";

	/**
	 * Whether to enable the console.
	 */
	private boolean enabled = false;

	private final Settings settings = new Settings();

	/**
	 * Returns the path of the H2 console.
	 * @return the path of the H2 console
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * Sets the path for the H2 console.
	 * @param path the path to set for the H2 console
	 * @throws IllegalArgumentException if the path is null or has a length less than or
	 * equal to 1
	 * @throws IllegalArgumentException if the path does not start with '/'
	 */
	public void setPath(String path) {
		Assert.notNull(path, "Path must not be null");
		Assert.isTrue(path.length() > 1, "Path must have length greater than 1");
		Assert.isTrue(path.startsWith("/"), "Path must start with '/'");
		this.path = path;
	}

	/**
	 * Returns the value of the enabled property.
	 * @return the value of the enabled property
	 */
	public boolean getEnabled() {
		return this.enabled;
	}

	/**
	 * Sets the enabled status of the H2 console.
	 * @param enabled the enabled status to be set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Returns the settings of the H2 console.
	 * @return the settings of the H2 console
	 */
	public Settings getSettings() {
		return this.settings;
	}

	/**
	 * Settings class.
	 */
	public static class Settings {

		/**
		 * Whether to enable trace output.
		 */
		private boolean trace = false;

		/**
		 * Whether to enable remote access.
		 */
		private boolean webAllowOthers = false;

		/**
		 * Password to access preferences and tools of H2 Console.
		 */
		private String webAdminPassword;

		/**
		 * Returns the value of the trace flag.
		 * @return true if the trace flag is enabled, false otherwise.
		 */
		public boolean isTrace() {
			return this.trace;
		}

		/**
		 * Sets the trace flag.
		 * @param trace the boolean value indicating whether to enable or disable tracing
		 */
		public void setTrace(boolean trace) {
			this.trace = trace;
		}

		/**
		 * Returns the value indicating whether the web server allows connections from
		 * other hosts.
		 * @return true if the web server allows connections from other hosts, false
		 * otherwise
		 */
		public boolean isWebAllowOthers() {
			return this.webAllowOthers;
		}

		/**
		 * Sets the value of the webAllowOthers property.
		 * @param webAllowOthers the new value for the webAllowOthers property
		 */
		public void setWebAllowOthers(boolean webAllowOthers) {
			this.webAllowOthers = webAllowOthers;
		}

		/**
		 * Returns the web admin password.
		 * @return the web admin password
		 */
		public String getWebAdminPassword() {
			return this.webAdminPassword;
		}

		/**
		 * Sets the password for the web admin.
		 * @param webAdminPassword the password for the web admin
		 */
		public void setWebAdminPassword(String webAdminPassword) {
			this.webAdminPassword = webAdminPassword;
		}

	}

}
