/*
 * Copyright 2012-2020 the original author or authors.
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

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		Assert.notNull(path, "Path must not be null");
		Assert.isTrue(path.length() > 1, "Path must have length greater than 1");
		Assert.isTrue(path.startsWith("/"), "Path must start with '/'");
		this.path = path;
	}

	public boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Settings getSettings() {
		return this.settings;
	}

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

		public boolean isTrace() {
			return this.trace;
		}

		public void setTrace(boolean trace) {
			this.trace = trace;
		}

		public boolean isWebAllowOthers() {
			return this.webAllowOthers;
		}

		public void setWebAllowOthers(boolean webAllowOthers) {
			this.webAllowOthers = webAllowOthers;
		}

		public String getWebAdminPassword() {
			return this.webAdminPassword;
		}

		public void setWebAdminPassword(String webAdminPassword) {
			this.webAdminPassword = webAdminPassword;
		}

	}

}
